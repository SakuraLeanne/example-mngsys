package com.example.mngsys.portal.security;

import com.example.mngsys.common.feign.dto.AuthSessionResponse;
import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.common.gateway.GatewaySecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
/**
 * AuthSessionInterceptor。
 */
public class AuthSessionInterceptor implements HandlerInterceptor {

    private static final String APP_MENU_PATH = "/app/menus";
    private static final String DEV_USER_HEADER = "X-User-Id";
    private static final String PTK_COOKIE_NAME = "ptk";
    private static final String SATOKEN_COOKIE_NAME = "satoken";
    private static final List<String> PTK_SCOPE_PATHS = Arrays.asList("/portal-server/password/change", "/portal-server/profile");

    private final AuthClient authClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final GatewaySecurityProperties gatewaySecurityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthSessionInterceptor(@org.springframework.context.annotation.Lazy AuthClient authClient,
                                  ObjectMapper objectMapper,
                                  Environment environment,
                                  GatewaySecurityProperties gatewaySecurityProperties) {
        this.authClient = authClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.gatewaySecurityProperties = gatewaySecurityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if (allowDevUserHeader(request)) {
            return true;
        }
        boolean requireSessionCheck = shouldRequireSessionCheck(request);
        if (!requireSessionCheck && isWhitelisted(request)) {
            return true;
        }
        String cookie = request.getHeader("Cookie");
        if (requireSessionCheck && !hasSaTokenCookie(cookie)) {
            writeUnauthorized(response, "登录凭证缺失，请先登录");
            return false;
        }
        if (cookie == null || cookie.trim().isEmpty()) {
            writeUnauthorized(response, "登录凭证缺失，请先登录");
            return false;
        }
        ResponseEntity<ApiResponse<AuthSessionResponse>> authResponse = authClient.sessionMe(cookie);
        ApiResponse<AuthSessionResponse> body = authResponse == null ? null : authResponse.getBody();
        if (body == null || body.getCode() != 0 || body.getData() == null) {
            writeUnauthorized(response, body == null ? null : body.getMessage());
            return false;
        }
        String userId = extractUserId(body.getData());
        if (userId == null) {
            writeUnauthorized(response, "登录凭证无效，请重新登录");
            return false;
        }
        RequestContext.setUserId(userId);
        request.setAttribute("userId", userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        List<String> whitelist = gatewaySecurityProperties.getWhitelist();

        return whitelist.stream().anyMatch(entry -> matches(entry, method, path));
    }

    private boolean shouldRequireSessionCheck(HttpServletRequest request) {
        if (!isPtkScopePath(request)) {
            return false;
        }
        return !hasPtkCookie(request);
    }

    private boolean isPtkScopePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PTK_SCOPE_PATHS.stream().anyMatch(path::equalsIgnoreCase);
    }

    private boolean hasPtkCookie(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return false;
        }
        for (javax.servlet.http.Cookie cookie : cookies) {
            if (PTK_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value != null && !value.trim().isEmpty();
            }
        }
        return false;
    }

    private boolean hasSaTokenCookie(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            return false;
        }
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith(SATOKEN_COOKIE_NAME + "=")) {
                String value = trimmed.substring(SATOKEN_COOKIE_NAME.length() + 1).trim();
                return !value.isEmpty();
            }
        }
        return false;
    }

    private boolean matches(String pattern, String method, String path) {
        String normalized = pattern.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        int firstSpace = normalized.indexOf(' ');
        if (firstSpace <= 0) {
            return matchesPath(normalized, path);
        }
        String configuredMethod = normalized.substring(0, firstSpace).trim();
        String configuredPath = normalized.substring(firstSpace + 1).trim();
        return configuredMethod.equalsIgnoreCase(method) && matchesPath(configuredPath, path);
    }

    private boolean matchesPath(String configuredPath, String requestPath) {
        if (pathMatcher.match(configuredPath, requestPath)) {
            return true;
        }
        String normalizedConfigured = normalizePortalPrefix(configuredPath);
        String normalizedRequest = normalizePortalPrefix(requestPath);
        if (configuredPath.equals(normalizedConfigured) && requestPath.equals(normalizedRequest)) {
            return false;
        }
        return pathMatcher.match(normalizedConfigured, normalizedRequest);
    }

    private String normalizePortalPrefix(String path) {
        if (path == null) {
            return null;
        }
        return path.replaceFirst("^/portal-server", "/portalserver");
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(ErrorCode.UNAUTHENTICATED.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        String resolvedMessage = StringUtils.hasText(message) ? message : ErrorCode.UNAUTHENTICATED.getMessage();
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.UNAUTHENTICATED, resolvedMessage));
    }

    private boolean allowDevUserHeader(HttpServletRequest request) {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            return false;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if (!APP_MENU_PATH.equalsIgnoreCase(path)) {
            return false;
        }
        String headerValue = request.getHeader(DEV_USER_HEADER);
        String userId = parseString(headerValue);
        if (userId == null) {
            return false;
        }
        RequestContext.setUserId(userId);
        request.setAttribute("userId", userId);
        return true;
    }

    private String extractUserId(Object data) {
        if (data instanceof AuthSessionResponse) {
            return ((AuthSessionResponse) data).getUserId();
        }
        if (data instanceof Map) {
            Object value = ((Map<?, ?>) data).get("userId");
            return parseString(value);
        }
        return null;
    }

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

}
