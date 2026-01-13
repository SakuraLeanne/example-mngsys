package com.example.mngsys.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.mngsys.common.redis.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * UserTokenVersionService。
 * <p>
 * 管理用户 Token 版本号，用于强制刷新登录态。
 * </p>
 */
@Service
public class UserTokenVersionService {

    private static final String SESSION_TOKEN_VERSION_KEY = "tokenVersion";

    private final StringRedisTemplate stringRedisTemplate;

    public UserTokenVersionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long getCurrentVersion(String userId) {
        if (!StringUtils.hasText(userId)) {
            return 0L;
        }
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.tokenVersion(userId));
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public void writeSessionVersion(long version) {
        StpUtil.getSession().set(SESSION_TOKEN_VERSION_KEY, version);
    }

    public Long getSessionVersion() {
        Object value = StpUtil.getSession().get(SESSION_TOKEN_VERSION_KEY);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    public boolean isSessionVersionValid(Long sessionVersion, long currentVersion) {
        return sessionVersion != null && sessionVersion == currentVersion;
    }
}
