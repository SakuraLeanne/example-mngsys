package com.dhgx.auth.service;

import com.dhgx.auth.common.api.ErrorCode;
import com.dhgx.auth.common.exception.LocalizedBusinessException;
import com.dhgx.auth.config.AuthProperties;
import com.dhgx.common.security.PasswordPolicyValidator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;

/**
 * PasswordResetService。
 * <p>
 * 负责忘记密码重置流程中的令牌生成、校验与清理。
 * </p>
 */
@Service
public class PasswordResetService {

    private static final String RESET_TOKEN_PREFIX = "auth:pwd:reset:hash:";
    private static final String RESET_TOKEN_GUARD_PREFIX = "auth:pwd:reset:guard:";
    private static final String RESET_TOKEN_FAIL_PREFIX = "auth:pwd:reset:fail:";

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthService authService;
    private final AuthProperties authProperties;

    public PasswordResetService(StringRedisTemplate stringRedisTemplate,
                                AuthService authService,
                                AuthProperties authProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.authService = authService;
        this.authProperties = authProperties;
    }

    /**
     * 生成重置令牌并写入缓存。
     *
     * @param mobile 手机号
     * @return 重置令牌
     */
    public String issueResetToken(String mobile) {
        guardIssueFrequency(mobile);
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttlSeconds = authProperties.getPasswordReset().getTokenTtlSeconds();
        String tokenHash = hash(token);
        stringRedisTemplate.opsForValue().set(buildResetKey(mobile), tokenHash, Duration.ofSeconds(ttlSeconds));
        stringRedisTemplate.opsForValue().set(buildGuardKey(mobile), "1",
                authProperties.getPasswordReset().getIssueIntervalSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        stringRedisTemplate.delete(buildFailKey(mobile));
        return token;
    }

    /**
     * 校验令牌并重置密码。
     *
     * @param mobile   手机号
     * @param token    重置令牌
     * @param password 明文密码
     */
    public void resetPassword(String mobile, String token, String password) {
        if (!StringUtils.hasText(mobile) || !StringUtils.hasText(token)) {
            throw new LocalizedBusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "error.reset.incomplete",
                    "重置信息不完整，请重试"
            );
        }
        if (exceedFailureLimit(mobile)) {
            throw new LocalizedBusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "error.reset.too-many-failures",
                    "重置失败次数过多，请重新获取验证码"
            );
        }
        String resetKey = buildResetKey(mobile);
        String cached = stringRedisTemplate.opsForValue().get(resetKey);
        if (!StringUtils.hasText(cached)) {
            throw new LocalizedBusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "error.reset.token.expired",
                    "重置令牌已失效，请重新获取验证码"
            );
        }
        String inputHash = hash(token);
        if (!MessageDigest.isEqual(inputHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                cached.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            incrementFailure(mobile, resetKey);
            throw new LocalizedBusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "error.reset.token.mismatch",
                    "重置验证失败，请重新获取验证码"
            );
        }
        if (!PasswordPolicyValidator.isValid(password)) {
            throw new LocalizedBusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "error.reset.password.policy",
                    "新密码不符合安全策略，请修改后重试"
            );
        }
        authService.resetPasswordByMobile(mobile, password);
        stringRedisTemplate.delete(resetKey);
        stringRedisTemplate.delete(buildFailKey(mobile));
    }

    private String buildResetKey(String mobile) {
        return RESET_TOKEN_PREFIX + mobile;
    }

    private String buildGuardKey(String mobile) {
        return RESET_TOKEN_GUARD_PREFIX + mobile;
    }

    private String buildFailKey(String mobile) {
        return RESET_TOKEN_FAIL_PREFIX + mobile;
    }

    private void guardIssueFrequency(String mobile) {
        String guardKey = buildGuardKey(mobile);
        if (StringUtils.hasText(stringRedisTemplate.opsForValue().get(guardKey))) {
            throw new LocalizedBusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "error.request.too-frequent",
                    "请求过于频繁，请稍后再试"
            );
        }
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("生成重置令牌失败", ex);
        }
    }

    private boolean exceedFailureLimit(String mobile) {
        String value = stringRedisTemplate.opsForValue().get(buildFailKey(mobile));
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            int failures = Integer.parseInt(value);
            return failures >= authProperties.getPasswordReset().getMaxVerifyFailures();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void incrementFailure(String mobile, String resetKey) {
        String failKey = buildFailKey(mobile);
        Long current = stringRedisTemplate.opsForValue().increment(failKey);
        Long ttl = stringRedisTemplate.getExpire(resetKey, java.util.concurrent.TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            stringRedisTemplate.expire(failKey, ttl, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (current != null && current >= authProperties.getPasswordReset().getMaxVerifyFailures()) {
            stringRedisTemplate.delete(resetKey);
        }
    }
}
