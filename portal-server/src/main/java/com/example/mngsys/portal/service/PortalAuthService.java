package com.example.mngsys.portal.service;

import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.exception.InvalidReturnUrlException;
import com.example.mngsys.portal.config.PortalProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
/**
 * PortalAuthService。
 */
public class PortalAuthService {

    private final AuthClient authClient;
    private final PortalProperties portalProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PortalAuthService(AuthClient authClient,
                             PortalProperties portalProperties,
                             StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper) {
        this.authClient = authClient;
        this.portalProperties = portalProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public LoginResult login(String username, String password, String systemCode, String returnUrl) {
        ResponseEntity<ApiResponse> response = authClient.loginWithResponse(username, password);
        ApiResponse body = response == null ? null : response.getBody();
        LoginResult result = new LoginResult(body, response);
        if (body != null && body.getCode() == 0 && StringUtils.hasText(systemCode) && StringUtils.hasText(returnUrl)) {
            String jumpUrl = createSsoJumpUrl(extractUserId(body.getData()), systemCode, returnUrl);
            result.setJumpUrl(jumpUrl);
        } else if (StringUtils.hasText(systemCode) ^ StringUtils.hasText(returnUrl)) {
            throw new IllegalArgumentException("systemCode 与 returnUrl 必须同时提供");
        }
        return result;
    }

    public ResponseEntity<ApiResponse> logout(String cookie) {
        return authClient.logoutWithResponse(cookie);
    }

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
            throw new IllegalStateException("生成 SSO ticket 失败", ex);
        }
    }

    private String buildTicketKey(String ticket) {
        return "sso:ticket:" + ticket;
    }

    private String extractUserId(Object data) {
        if (data instanceof Map) {
            Object value = ((Map<?, ?>) data).get("userId");
            if (value != null) {
                String text = value.toString();
                return StringUtils.hasText(text) ? text : null;
            }
        }
        return null;
    }

    public static class LoginResult {
        private final ApiResponse responseBody;
        private final ResponseEntity<ApiResponse> responseEntity;
        private String jumpUrl;

        public LoginResult(ApiResponse responseBody, ResponseEntity<ApiResponse> responseEntity) {
            this.responseBody = responseBody;
            this.responseEntity = responseEntity;
        }

        public ApiResponse getResponseBody() {
            return responseBody;
        }

        public ResponseEntity<ApiResponse> getResponseEntity() {
            return responseEntity;
        }

        public String getJumpUrl() {
            return jumpUrl;
        }

        public void setJumpUrl(String jumpUrl) {
            this.jumpUrl = jumpUrl;
        }
    }
}
