package com.example.mngsys.auth.service;

import com.example.mngsys.auth.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * SmsCodeService。
 * <p>
 * 负责短信验证码的生成、存储、下发与校验。
 * </p>
 */
@Service
public class SmsCodeService {

    private static final String CODE_KEY_PREFIX = "auth:sms:code:";
    private static final String SEND_GUARD_PREFIX = "auth:sms:send:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final StringRedisTemplate stringRedisTemplate;
    private final AliyunSendMsgUtils aliyunSendMsgUtils;
    private final AuthProperties authProperties;

    public SmsCodeService(StringRedisTemplate stringRedisTemplate,
                          AliyunSendMsgUtils aliyunSendMsgUtils,
                          AuthProperties authProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.aliyunSendMsgUtils = aliyunSendMsgUtils;
        this.authProperties = authProperties;
    }

    /**
     * 生成并发送短信验证码。
     *
     * @param mobile 手机号
     * @param scene  模板场景
     */
    public void sendCode(String mobile, TemplateScene scene) {
        guardSendFrequency(mobile);
        String code = generateCode(authProperties.getSms().getCodeLength());
        Duration ttl = Duration.ofSeconds(authProperties.getSms().getTtlSeconds());
        stringRedisTemplate.opsForValue().set(buildCodeKey(mobile), code, ttl);
        stringRedisTemplate.opsForValue().set(buildSendGuardKey(mobile), "1",
                authProperties.getSms().getSendIntervalSeconds(), TimeUnit.SECONDS);
        System.out.println("================= 短信验证码 : "+code+" ================= ");
//        aliyunSendMsgUtils.sendCode(mobile, code, resolveTemplateCode(scene));
    }

    /**
     * 默认登录场景的短信验证码发送入口，兼容旧调用。
     */
    public void sendCode(String mobile) {
        sendCode(mobile, TemplateScene.LOGIN);
    }

    /**
     * 校验验证码并在成功后删除。
     *
     * @param mobile 手机号
     * @param code   验证码
     */
    public void verifyCode(String mobile, String code) {
        String key = buildCodeKey(mobile);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(cached)) {
            throw new IllegalArgumentException("验证码已失效或未发送");
        }
        if (!cached.equals(code)) {
            throw new IllegalArgumentException("验证码错误");
        }
        stringRedisTemplate.delete(key);
    }

    private String generateCode(int length) {
        int bound = (int) Math.pow(10, length);
        int floor = (int) Math.pow(10, length - 1);
        int value = secureRandom.nextInt(bound - floor) + floor;
        return String.format("%0" + length + "d", value);
    }

    private void guardSendFrequency(String mobile) {
        String guardKey = buildSendGuardKey(mobile);
        String exists = stringRedisTemplate.opsForValue().get(guardKey);
        if (StringUtils.hasText(exists)) {
            throw new IllegalArgumentException("请求过于频繁，请稍后再试");
        }
    }

    private String buildCodeKey(String mobile) {
        return CODE_KEY_PREFIX + mobile;
    }

    private String buildSendGuardKey(String mobile) {
        return SEND_GUARD_PREFIX + mobile;
    }

    private String resolveTemplateCode(TemplateScene scene) {
        AuthProperties.SmsProperties.TemplateCodeProperties templateCode = authProperties.getSms().getTemplateCode();
        if (templateCode == null) {
            throw new IllegalStateException("短信模板未配置");
        }
        String resolved;
        switch (scene) {
            case LOGIN:
                resolved = templateCode.getLoginVerificationCode();
                if (!StringUtils.hasText(resolved)) {
                    resolved = templateCode.getVerificationCode();
                }
                break;
            case VERIFICATION:
            default:
                resolved = templateCode.getVerificationCode();
                if (!StringUtils.hasText(resolved)) {
                    resolved = templateCode.getLoginVerificationCode();
                }
                break;
        }
        if (!StringUtils.hasText(resolved)) {
            throw new IllegalStateException("短信模板未配置");
        }
        return resolved;
    }

    public enum TemplateScene {
        LOGIN,
        VERIFICATION
    }
}
