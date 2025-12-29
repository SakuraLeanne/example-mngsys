package com.example.mngsys.portal.service.impl;

import com.example.mngsys.portal.config.PortalProperties;
import com.example.mngsys.portal.service.UserAuthCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
/**
 * UserAuthCacheServiceImpl。
 */
public class UserAuthCacheServiceImpl implements UserAuthCacheService {

    private static final String USER_AUTH_PREFIX = "user:auth:";

    private final StringRedisTemplate stringRedisTemplate;
    private final PortalProperties portalProperties;

    public UserAuthCacheServiceImpl(StringRedisTemplate stringRedisTemplate,
                                    PortalProperties portalProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.portalProperties = portalProperties;
    }

    @Override
    public void updateUserAuthCache(String userId, Integer status, Long authVersion, Long profileVersion) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        Map<String, String> values = new HashMap<>();
        if (status != null) {
            values.put("status", status.toString());
        }
        if (authVersion != null) {
            values.put("authVersion", authVersion.toString());
        }
        if (profileVersion != null) {
            values.put("profileVersion", profileVersion.toString());
        }
        if (values.isEmpty()) {
            return;
        }
        String key = buildKey(userId);
        stringRedisTemplate.opsForHash().putAll(key, values);
        long ttlSeconds = portalProperties.getUserAuth().getCacheTtlSeconds();
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public UserAuthCache getUserAuthCache(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(buildKey(userId));
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        Integer status = parseInt(entries.get("status"));
        Long authVersion = parseLong(entries.get("authVersion"));
        Long profileVersion = parseLong(entries.get("profileVersion"));
        return new UserAuthCache(status, authVersion, profileVersion);
    }

    private String buildKey(String userId) {
        return USER_AUTH_PREFIX + userId;
    }

    private Integer parseInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
