package com.dhgx.portal.service;

import com.dhgx.common.feign.dto.AuthLoginRequest;
import com.dhgx.common.feign.dto.AuthLoginResponse;
import com.dhgx.common.feign.dto.AuthLoginType;
import com.dhgx.common.portal.dto.PortalLoginRequest;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.common.SsoTicketUtils;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.exception.InvalidReturnUrlException;
import com.dhgx.portal.common.exception.LocalizedBusinessException;
import com.dhgx.portal.config.PortalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PortalAuthService.class);
    private static final String LOGIN_FAIL_PREFIX = "PORTAL:LOGIN_FAIL:";
    private static final long MIN_TICKET_TTL_SECONDS = 30;
    private static final long MAX_TICKET_TTL_SECONDS = 120;
    private static final long DEFAULT_TICKET_TTL_SECONDS = 60;

    /** 认证中心客户端。 */
    private final AuthClient authClient;
    /** 门户配置。 */
    private final PortalProperties portalProperties;
    /** Redis 模板。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 验证码服务。 */
    private final CaptchaService captchaService;

    /**
     * 构造函数，注入依赖。
     */
    public PortalAuthService(AuthClient authClient,
                             PortalProperties portalProperties,
                             StringRedisTemplate stringRedisTemplate,
                             CaptchaService captchaService) {
        this.authClient = authClient;
        this.portalProperties = portalProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.captchaService = captchaService;
    }

    /**
     * 登录操作，必要时生成单点登录跳转地址。
     */
    public LoginResult login(PortalLoginRequest loginRequest) {
        return login(loginRequest, null);
    }

    /**
     * 登录操作，必要时生成单点登录跳转地址。
     */
    public LoginResult login(PortalLoginRequest loginRequest, String clientIp) {
        if (loginRequest == null) {
            throw new IllegalArgumentException("登录请求不能为空");
        }
        AuthLoginType loginType = loginRequest.getLoginTypeOrDefault();
        if (isUsernamePassword(loginType)) {
            enforceCaptchaIfNeeded(loginRequest, clientIp);
        }
        AuthLoginRequest authLoginRequest = new AuthLoginRequest();
        authLoginRequest.setLoginType(loginType);
        authLoginRequest.setMobile(loginRequest.getMobile());
        authLoginRequest.setCode(loginRequest.getCode());
        authLoginRequest.setUsername(loginRequest.getUsername());
        authLoginRequest.setPassword(loginRequest.getPassword());
        authLoginRequest.setEncryptedPassword(loginRequest.getEncryptedPassword());

        ResponseEntity<ApiResponse<AuthLoginResponse>> response = authClient.loginWithResponse(authLoginRequest);
        ApiResponse<AuthLoginResponse> body = response == null ? null : response.getBody();
        if (isUsernamePassword(loginType)) {
            handleLoginAttempt(loginRequest.getUsername(), clientIp, body);
        }
        LoginResult result = new LoginResult(body, response);
        String systemCode = loginRequest.getSystemCode();
        String returnUrl = loginRequest.getReturnUrl();
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
    public ResponseEntity<ApiResponse<Void>> logout(String cookie) {
        return authClient.logoutWithResponse(cookie);
    }

    /**
     * 创建单点登录跳转地址并写入 Ticket 缓存。
     */
    public String createSsoJumpUrl(String userId, String systemCode, String targetUrl) {
        validateReturnUrl(targetUrl);
        if (userId == null) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        Instant issuedAt = Instant.now();
        long ttlSeconds = normalizeTicketTtlSeconds(portalProperties.getSso().getTicketTtlSeconds());
        Instant expireAt = issuedAt.plusSeconds(ttlSeconds);
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("systemCode", systemCode);
        payload.put("issuedAt", String.valueOf(issuedAt.toEpochMilli()));
        payload.put("expireAt", String.valueOf(expireAt.toEpochMilli()));
        payload.put("redirectUriHash", SsoTicketUtils.hashRedirectUri(targetUrl));
        payload.put("stateHash", "");
        String key = buildTicketKey(ticket);
        stringRedisTemplate.opsForHash().putAll(key, payload);
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
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
            throw new InvalidReturnUrlException("回调地址不合法或不在白名单");
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
     * 构建 Ticket 的缓存键。
     */
    private String buildTicketKey(String ticket) {
        return SsoTicketUtils.buildTicketKey(ticket);
    }

    private long normalizeTicketTtlSeconds(long ttlSeconds) {
        if (ttlSeconds >= MIN_TICKET_TTL_SECONDS && ttlSeconds <= MAX_TICKET_TTL_SECONDS) {
            return ttlSeconds;
        }
        log.warn("SSO ticket TTL out of range ({}). Use default {} seconds.", ttlSeconds, DEFAULT_TICKET_TTL_SECONDS);
        return DEFAULT_TICKET_TTL_SECONDS;
    }

    private void enforceCaptchaIfNeeded(PortalLoginRequest loginRequest, String clientIp) {
        PortalProperties.Security.Captcha captcha = portalProperties.getSecurity().getCaptcha();
        if (!captcha.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(loginRequest.getCaptchaId())
                || !StringUtils.hasText(loginRequest.getCaptchaCode())) {
            throw new LocalizedBusinessException(ErrorCode.CAPTCHA_REQUIRED,
                    "error.captcha.required",
                    ErrorCode.CAPTCHA_REQUIRED.getMessage());
        }
        captchaService.verifyCaptcha(loginRequest.getCaptchaId(), loginRequest.getCaptchaCode());
    }

    private void handleLoginAttempt(String username, String clientIp, ApiResponse<?> body) {
        String loginKey = buildLoginKey(username, clientIp);
        if (!StringUtils.hasText(loginKey)) {
            return;
        }
        if (body != null && body.getCode() == 0) {
            stringRedisTemplate.delete(buildLoginFailKey(loginKey));
            return;
        }
        PortalProperties.Security.Captcha captcha = portalProperties.getSecurity().getCaptcha();
        String key = buildLoginFailKey(loginKey);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, captcha.getFailWindowSeconds(), TimeUnit.SECONDS);
        }
    }

    private String buildLoginKey(String username, String clientIp) {
        String normalizedUser = normalizeUsername(username);
        String normalizedIp = clientIp == null ? "" : clientIp.trim();
        if (!StringUtils.hasText(normalizedUser) && !StringUtils.hasText(normalizedIp)) {
            return null;
        }
        return normalizedUser + ":" + normalizedIp;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String buildLoginFailKey(String loginKey) {
        return LOGIN_FAIL_PREFIX + loginKey;
    }

    private boolean isUsernamePassword(AuthLoginType loginType) {
        return AuthLoginType.USERNAME_PASSWORD == loginType;
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
