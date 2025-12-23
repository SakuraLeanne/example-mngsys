package com.example.mngsys.portal.service;

import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.exception.InvalidReturnUrlException;
import com.example.mngsys.portal.config.PortalProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PortalActionService {

    private static final String ACTION_TICKET_PASSWORD_PREFIX = "portal:action:ticket:pwd:";
    private static final String ACTION_TICKET_PROFILE_PREFIX = "portal:action:ticket:profile:";
    private static final String PTK_PREFIX = "portal:ptk:";

    private final PortalProperties portalProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PortalActionService(PortalProperties portalProperties,
                               StringRedisTemplate stringRedisTemplate,
                               ObjectMapper objectMapper) {
        this.portalProperties = portalProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public EnterResult enter(ActionType actionType, String ticket) {
        String key = actionType.buildTicketKey(ticket);
        String payload = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(payload)) {
            return EnterResult.failure(ErrorCode.ACTION_TICKET_INVALID);
        }
        ActionTicketPayload ticketPayload = parseTicketPayload(payload);
        if (ticketPayload == null) {
            return EnterResult.failure(ErrorCode.ACTION_TICKET_INVALID);
        }
        if (ticketPayload.isExpired()) {
            return EnterResult.failure(ErrorCode.ACTION_TICKET_EXPIRED);
        }
        if (!StringUtils.hasText(ticketPayload.getReturnUrl()) || !StringUtils.hasText(ticketPayload.getSourceSystemCode())) {
            return EnterResult.failure(ErrorCode.ACTION_TICKET_INVALID);
        }
        validateReturnUrl(ticketPayload.getReturnUrl());
        Boolean deleted = stringRedisTemplate.delete(key);
        if (deleted == null || !deleted) {
            return EnterResult.failure(ErrorCode.ACTION_TICKET_REPLAYED);
        }
        String ptk = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> ptkPayload = new HashMap<>();
        ptkPayload.put("userId", ticketPayload.getUserId());
        ptkPayload.put("scope", ticketPayload.getScope(actionType));
        ptkPayload.put("returnUrl", ticketPayload.getReturnUrl());
        ptkPayload.put("sourceSystemCode", ticketPayload.getSourceSystemCode());
        long ttlSeconds = portalProperties.getPtk().getTtlSeconds();
        stringRedisTemplate.opsForValue().set(buildPtkKey(ptk), toJson(ptkPayload), ttlSeconds, TimeUnit.SECONDS);
        return EnterResult.success(ptk, ticketPayload.getReturnUrl(), ticketPayload.getSourceSystemCode());
    }

    private ActionTicketPayload parseTicketPayload(String payload) {
        try {
            Map<String, Object> value = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            return ActionTicketPayload.from(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void validateReturnUrl(String returnUrl) {
        if (!isAllowedHost(returnUrl)) {
            throw new InvalidReturnUrlException("非法回调地址");
        }
    }

    private boolean isAllowedHost(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(returnUrl);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            return false;
        }
        List<String> allowedHosts = portalProperties.getSecurity().getAllowedHosts();
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return allowedHosts.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生成 PTK 失败", ex);
        }
    }

    private String buildPtkKey(String ptk) {
        return PTK_PREFIX + ptk;
    }

    public enum ActionType {
        PASSWORD("pwd", ACTION_TICKET_PASSWORD_PREFIX),
        PROFILE("profile", ACTION_TICKET_PROFILE_PREFIX);

        private final String scope;
        private final String prefix;

        ActionType(String scope, String prefix) {
            this.scope = scope;
            this.prefix = prefix;
        }

        public String getScope() {
            return scope;
        }

        public String buildTicketKey(String ticket) {
            return prefix + ticket;
        }
    }

    public static class EnterResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String ptk;
        private final String returnUrl;
        private final String systemCode;

        private EnterResult(boolean success, ErrorCode errorCode, String ptk, String returnUrl, String systemCode) {
            this.success = success;
            this.errorCode = errorCode;
            this.ptk = ptk;
            this.returnUrl = returnUrl;
            this.systemCode = systemCode;
        }

        public static EnterResult success(String ptk, String returnUrl, String systemCode) {
            return new EnterResult(true, null, ptk, returnUrl, systemCode);
        }

        public static EnterResult failure(ErrorCode errorCode) {
            return new EnterResult(false, errorCode, null, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getPtk() {
            return ptk;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public String getSystemCode() {
            return systemCode;
        }
    }

    private static class ActionTicketPayload {
        private final Long userId;
        private final String scope;
        private final String returnUrl;
        private final String sourceSystemCode;
        private final Long expireAt;

        private ActionTicketPayload(Long userId, String scope, String returnUrl, String sourceSystemCode, Long expireAt) {
            this.userId = userId;
            this.scope = scope;
            this.returnUrl = returnUrl;
            this.sourceSystemCode = sourceSystemCode;
            this.expireAt = expireAt;
        }

        public static ActionTicketPayload from(Map<String, Object> data) {
            if (data == null || data.isEmpty()) {
                return null;
            }
            Long userId = parseLong(data.get("userId"));
            String scope = parseString(data.get("scope"));
            String returnUrl = parseString(data.get("returnUrl"));
            String sourceSystemCode = parseString(data.get("sourceSystemCode"));
            Long expireAt = parseLong(data.get("expireAt"));
            if (userId == null) {
                return null;
            }
            return new ActionTicketPayload(userId, scope, returnUrl, sourceSystemCode, expireAt);
        }

        public boolean isExpired() {
            if (expireAt == null) {
                return false;
            }
            return Instant.now().toEpochMilli() > expireAt;
        }

        public Long getUserId() {
            return userId;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public String getSourceSystemCode() {
            return sourceSystemCode;
        }

        public String getScope(ActionType actionType) {
            return StringUtils.hasText(scope) ? scope : actionType.getScope();
        }

        private static Long parseLong(Object value) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value != null) {
                try {
                    return Long.parseLong(value.toString());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        }

        private static String parseString(Object value) {
            return value == null ? null : value.toString();
        }
    }
}
