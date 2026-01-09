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

/**
 * PortalActionService。
 * <p>
 * 负责 Action Ticket、PTK 的校验与生成，用于门户在账号安全等场景下的跳转授权。
 * </p>
 */
@Service
public class PortalActionService {

    /** 修改密码 action_ticket 前缀。 */
    private static final String ACTION_TICKET_PASSWORD_PREFIX = "portal:action:ticket:pwd:";
    /** 修改资料 action_ticket 前缀。 */
    private static final String ACTION_TICKET_PROFILE_PREFIX = "portal:action:ticket:profile:";
    /** Portal Token 缓存前缀。 */
    private static final String PTK_PREFIX = "portal:ptk:";

    /** 门户配置。 */
    private final PortalProperties portalProperties;
    /** Redis 模板。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入依赖。
     */
    public PortalActionService(PortalProperties portalProperties,
                               StringRedisTemplate stringRedisTemplate,
                               ObjectMapper objectMapper) {
        this.portalProperties = portalProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验 action_ticket 并生成 PTK。
     *
     * @param actionType 票据类型
     * @param ticket     action_ticket 字符串
     * @return 封装结果
     */
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

    /**
     * 将缓存的 JSON 字符串解析为票据对象。
     */
    private ActionTicketPayload parseTicketPayload(String payload) {
        try {
            Map<String, Object> value = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            return ActionTicketPayload.from(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /**
     * 校验回调地址是否在白名单。
     */
    private void validateReturnUrl(String returnUrl) {
        if (!isAllowedHost(returnUrl)) {
            throw new InvalidReturnUrlException("非法回调地址");
        }
    }

    /**
     * 判断 host 是否允许。
     */
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

    /**
     * 序列化对象为 JSON。
     */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生成 PTK 失败", ex);
        }
    }

    /**
     * 构建 PTK 缓存键。
     */
    private String buildPtkKey(String ptk) {
        return PTK_PREFIX + ptk;
    }

    /**
     * ActionTicket 类型枚举。
     */
    public enum ActionType {
        /** 密码相关票据。 */
        PASSWORD("PWD_CHANGE", ACTION_TICKET_PASSWORD_PREFIX),
        /** 资料相关票据。 */
        PROFILE("PROFILE_EDIT", ACTION_TICKET_PROFILE_PREFIX);

        /** 对应的 scope。 */
        private final String scope;
        /** Redis 键前缀。 */
        private final String prefix;

        ActionType(String scope, String prefix) {
            this.scope = scope;
            this.prefix = prefix;
        }

        /** 获取 scope。 */
        public String getScope() {
            return scope;
        }

        /** 构建票据缓存键。 */
        public String buildTicketKey(String ticket) {
            return prefix + ticket;
        }
    }

    /**
     * 进入 Action 的结果封装。
     */
    public static class EnterResult {
        /** 是否成功。 */
        private final boolean success;
        /** 错误码。 */
        private final ErrorCode errorCode;
        /** 生成的 PTK。 */
        private final String ptk;
        /** 回跳地址。 */
        private final String returnUrl;
        /** 来源系统编码。 */
        private final String systemCode;

        private EnterResult(boolean success, ErrorCode errorCode, String ptk, String returnUrl, String systemCode) {
            this.success = success;
            this.errorCode = errorCode;
            this.ptk = ptk;
            this.returnUrl = returnUrl;
            this.systemCode = systemCode;
        }

        /** 构建成功结果。 */
        public static EnterResult success(String ptk, String returnUrl, String systemCode) {
            return new EnterResult(true, null, ptk, returnUrl, systemCode);
        }

        /** 构建失败结果。 */
        public static EnterResult failure(ErrorCode errorCode) {
            return new EnterResult(false, errorCode, null, null, null);
        }

        /** 是否成功。 */
        public boolean isSuccess() {
            return success;
        }

        /** 获取错误码。 */
        public ErrorCode getErrorCode() {
            return errorCode;
        }

        /** 获取 PTK。 */
        public String getPtk() {
            return ptk;
        }

        /** 获取回跳地址。 */
        public String getReturnUrl() {
            return returnUrl;
        }

        /** 获取系统编码。 */
        public String getSystemCode() {
            return systemCode;
        }
    }

    /**
     * action_ticket 载荷。
     */
    private static class ActionTicketPayload {
        /** 用户 ID。 */
        private final String userId;
        /** 作用域。 */
        private final String scope;
        /** 回跳地址。 */
        private final String returnUrl;
        /** 来源系统编码。 */
        private final String sourceSystemCode;
        /** 过期时间戳。 */
        private final Long expireAt;

        private ActionTicketPayload(String userId, String scope, String returnUrl, String sourceSystemCode, Long expireAt) {
            this.userId = userId;
            this.scope = scope;
            this.returnUrl = returnUrl;
            this.sourceSystemCode = sourceSystemCode;
            this.expireAt = expireAt;
        }

        /** 从 Map 构建载荷对象。 */
        public static ActionTicketPayload from(Map<String, Object> data) {
            if (data == null || data.isEmpty()) {
                return null;
            }
            String userId = parseString(data.get("userId"));
            String scope = parseString(data.get("scope"));
            String returnUrl = parseString(data.get("returnUrl"));
            String sourceSystemCode = parseString(data.get("sourceSystemCode"));
            Long expireAt = parseLong(data.get("expireAt"));
            if (userId == null) {
                return null;
            }
            return new ActionTicketPayload(userId, scope, returnUrl, sourceSystemCode, expireAt);
        }

        /** 判断是否过期。 */
        public boolean isExpired() {
            if (expireAt == null) {
                return false;
            }
            return Instant.now().getEpochSecond() > expireAt;
        }

        /** 获取用户 ID。 */
        public String getUserId() {
            return userId;
        }

        /** 获取回跳地址。 */
        public String getReturnUrl() {
            return returnUrl;
        }

        /** 获取来源系统。 */
        public String getSourceSystemCode() {
            return sourceSystemCode;
        }

        /** 按优先级获取作用域。 */
        public String getScope(ActionType actionType) {
            return StringUtils.hasText(scope) ? scope : actionType.getScope();
        }

        /** 解析 Long。 */
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

        /** 解析字符串。 */
        private static String parseString(Object value) {
            if (value == null) {
                return null;
            }
            String text = value.toString();
            return StringUtils.hasText(text) ? text : null;
        }
    }
}
