package com.example.mngsys.portal.service;

import com.example.mngsys.common.feign.dto.AuthLoginResponse;
import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.exception.InvalidReturnUrlException;
import com.example.mngsys.portal.config.PortalProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PortalAuthService。
 * <p>
 * 负责对接认证中心完成登录、登出，并提供 SSO Ticket 生成与校验。
 * </p>
 */
@Service
public class PortalAuthService {

    /** 认证中心客户端。 */
    private final AuthClient authClient;
    /** 门户配置。 */
    private final PortalProperties portalProperties;
    /** Redis 模板。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入依赖。
     */
    public PortalAuthService(AuthClient authClient,
                             PortalProperties portalProperties,
                             StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper) {
        this.authClient = authClient;
        this.portalProperties = portalProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 登录操作，必要时生成单点登录跳转地址。
     */
    public LoginResult login(String mobile, String code, String systemCode, String returnUrl) {
        ResponseEntity<ApiResponse<AuthLoginResponse>> response = authClient.loginWithResponse(mobile, code);
        ApiResponse<AuthLoginResponse> body = response == null ? null : response.getBody();
        LoginResult result = new LoginResult(body, response);
        if (body != null && body.getCode() == 0 && StringUtils.hasText(systemCode) && StringUtils.hasText(returnUrl)) {
            String jumpUrl = createSsoJumpUrl(extractUserId(body.getData()), systemCode, returnUrl);
            result.setJumpUrl(jumpUrl);
        } else if (StringUtils.hasText(systemCode) ^ StringUtils.hasText(returnUrl)) {
            throw new IllegalArgumentException("systemCode 与 returnUrl 必须同时提供");
        }
        return result;
    }

    /**
     * 调用认证中心登出接口。
     */
    public ResponseEntity<ApiResponse> logout(String cookie) {
        return authClient.logoutWithResponse(cookie);
    }

    /**
     * 创建单点登录跳转地址并写入 Ticket 缓存。
     */
    public String createSsoJumpUrl(String userId, String systemCode, String targetUrl) {
        validateReturnUrl(targetUrl);
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        Instant issuedAt = Instant.now();
        long ttlSeconds = portalProperties.getSso().getTicketTtlSeconds();
        Instant expireAt = issuedAt.plusSeconds(ttlSeconds);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("systemCode", systemCode);
        payload.put("issuedAt", issuedAt.toEpochMilli());
        payload.put("expireAt", expireAt.toEpochMilli());
        String value = toJson(payload);
        stringRedisTemplate.opsForValue().set(buildTicketKey(ticket), value, ttlSeconds, TimeUnit.SECONDS);
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("ticket", ticket)
                .build()
                .toUriString();
    }

    /**
     * 校验回调地址合法性。
     */
    private void validateReturnUrl(String returnUrl) {
        if (!isAllowedHost(returnUrl)) {
            throw new InvalidReturnUrlException("非法回调地址");
        }
    }

    /**
     * 判断回调 host 是否在白名单。
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
     * 对象转 JSON 字符串。
     */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生成 SSO ticket 失败", ex);
        }
    }

    /**
     * 构建 Ticket 的缓存键。
     */
    private String buildTicketKey(String ticket) {
        return "sso:ticket:" + ticket;
    }

    /**
     * 从登录返回中解析用户 ID。
     */
    private String extractUserId(Object data) {
        if (data instanceof AuthLoginResponse) {
            String userId = ((AuthLoginResponse) data).getUserId();
            return StringUtils.hasText(userId) ? userId : null;
        }
        if (data instanceof Map) {
            Object value = ((Map<?, ?>) data).get("userId");
            if (value != null) {
                String text = value.toString();
                return StringUtils.hasText(text) ? text : null;
            }
        }
        return null;
    }

    /**
     * 登录结果封装。
     */
    public static class LoginResult {
        /** 登录响应体。 */
        private final ApiResponse<?> responseBody;
        /** 原始 ResponseEntity。 */
        private final ResponseEntity<? extends ApiResponse<?>> responseEntity;
        /** 单点登录跳转地址。 */
        private String jumpUrl;

        public LoginResult(ApiResponse<?> responseBody, ResponseEntity<? extends ApiResponse<?>> responseEntity) {
            this.responseBody = responseBody;
            this.responseEntity = responseEntity;
        }

        /** 获取响应体。 */
        public ApiResponse<?> getResponseBody() {
            return responseBody;
        }

        /** 获取 ResponseEntity。 */
        public ResponseEntity<? extends ApiResponse<?>> getResponseEntity() {
            return responseEntity;
        }

        /** 获取跳转地址。 */
        public String getJumpUrl() {
            return jumpUrl;
        }

        /** 设置跳转地址。 */
        public void setJumpUrl(String jumpUrl) {
            this.jumpUrl = jumpUrl;
        }
    }
}
