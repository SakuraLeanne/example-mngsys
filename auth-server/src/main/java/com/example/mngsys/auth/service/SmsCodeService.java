package com.example.mngsys.auth.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teautil.Common;
import com.example.mngsys.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SmsCodeService。
 * <p>
 * 负责短信验证码的生成、存储、下发与校验。
 * </p>
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    private static final String CODE_KEY_PREFIX = "auth:sms:code:";
    private static final String SEND_GUARD_PREFIX = "auth:sms:send:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final StringRedisTemplate stringRedisTemplate;
    private final Client smsClient;
    private final AuthProperties authProperties;

    public SmsCodeService(StringRedisTemplate stringRedisTemplate,
                          Client smsClient,
                          AuthProperties authProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.smsClient = smsClient;
        this.authProperties = authProperties;
    }

    /**
     * 生成并发送短信验证码。
     *
     * @param mobile 手机号
     */
    public void sendCode(String mobile) {
        guardSendFrequency(mobile);
        String code = generateCode(authProperties.getSms().getCodeLength());
        Duration ttl = Duration.ofSeconds(authProperties.getSms().getTtlSeconds());
        stringRedisTemplate.opsForValue().set(buildCodeKey(mobile), code, ttl);
        stringRedisTemplate.opsForValue().set(buildSendGuardKey(mobile), "1",
                authProperties.getSms().getSendIntervalSeconds(), TimeUnit.SECONDS);
        sendSms(mobile, code);
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

    private void sendSms(String mobile, String code) {
        AuthProperties.SmsProperties sms = authProperties.getSms();
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("code", code);
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(mobile)
                    .setSignName(sms.getSignName())
                    .setTemplateCode(sms.getTemplateCode())
                    .setTemplateParam(Common.toJSONString(templateParams));
            smsClient.sendSms(request);
        } catch (Exception ex) {
            log.error("发送短信验证码失败，mobile={}", mobile, ex);
            throw new IllegalStateException("短信发送失败，请稍后重试");
        }
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
}
