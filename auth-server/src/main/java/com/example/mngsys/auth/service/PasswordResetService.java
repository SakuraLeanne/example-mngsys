package com.example.mngsys.auth.service;

import com.example.mngsys.auth.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private static final String RESET_TOKEN_PREFIX = "auth:pwd:reset:";

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
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttlSeconds = authProperties.getPasswordReset().getTokenTtlSeconds();
        stringRedisTemplate.opsForValue().set(buildResetKey(mobile), token, Duration.ofSeconds(ttlSeconds));
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
            throw new IllegalArgumentException("重置信息不完整");
        }
        String cached = stringRedisTemplate.opsForValue().get(buildResetKey(mobile));
        if (!StringUtils.hasText(cached)) {
            throw new IllegalArgumentException("重置令牌已失效");
        }
        if (!cached.equals(token)) {
            throw new IllegalArgumentException("重置令牌不匹配");
        }
        authService.resetPasswordByMobile(mobile, password);
        stringRedisTemplate.delete(buildResetKey(mobile));
    }

    private String buildResetKey(String mobile) {
        return RESET_TOKEN_PREFIX + mobile;
    }
}
