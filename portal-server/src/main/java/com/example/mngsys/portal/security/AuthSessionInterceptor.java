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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
/**
 * AuthSessionInterceptor。
 */
public class AuthSessionInterceptor implements HandlerInterceptor {

    private static final String APP_MENU_PATH = "/portal/api/app/menus";
    private static final String DEV_USER_HEADER = "X-User-Id";


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
        if (isWhitelisted(request)) {
            return true;
        }
        String cookie = request.getHeader("Cookie");
        if (cookie == null || cookie.trim().isEmpty()) {
            writeUnauthorized(response);
            return false;
        }
        ResponseEntity<ApiResponse<AuthSessionResponse>> authResponse = authClient.sessionMe(cookie);
        ApiResponse<AuthSessionResponse> body = authResponse == null ? null : authResponse.getBody();
        if (body == null || body.getCode() != 0 || body.getData() == null) {
            writeUnauthorized(response);
            return false;
        }
        String userId = extractUserId(body.getData());
        if (userId == null) {
            writeUnauthorized(response);
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

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.UNAUTHENTICATED.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.UNAUTHENTICATED));
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
