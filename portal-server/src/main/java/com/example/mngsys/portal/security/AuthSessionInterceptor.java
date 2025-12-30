package com.example.mngsys.portal.security;

import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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

    private static final List<WhitelistEntry> WHITELIST = Arrays.asList(
            new WhitelistEntry("POST", "/portal/api/login"),
            new WhitelistEntry("POST", "/portal/api/action/pwd/enter"),
            new WhitelistEntry("POST", "/portal/api/action/profile/enter"),
            new WhitelistEntry("POST", "/portal/api/password/change"),
            new WhitelistEntry("GET", "/portal/api/profile"),
            new WhitelistEntry("POST", "/portal/api/profile")
    );
    private static final String APP_MENU_PATH = "/portal/api/app/menus";
    private static final String DEV_USER_HEADER = "X-User-Id";

    private final AuthClient authClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public AuthSessionInterceptor(@org.springframework.context.annotation.Lazy AuthClient authClient,
                                  ObjectMapper objectMapper,
                                  Environment environment) {
        this.authClient = authClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
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
        ResponseEntity<ApiResponse> authResponse = authClient.sessionMe(cookie);
        ApiResponse body = authResponse == null ? null : authResponse.getBody();
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
        return WHITELIST.stream().anyMatch(entry -> entry.matches(method, path));
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
        if (data instanceof AuthClient.SessionResponse) {
            return ((AuthClient.SessionResponse) data).getUserId();
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

    private static class WhitelistEntry {
        private final String method;
        private final String path;

        private WhitelistEntry(String method, String path) {
            this.method = method;
            this.path = path;
        }

        private boolean matches(String method, String path) {
            return this.method.equalsIgnoreCase(method) && this.path.equalsIgnoreCase(path);
        }
    }
}
