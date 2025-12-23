package com.example.mngsys.portal.security;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PtkInterceptor implements HandlerInterceptor {

    private static final String PTK_COOKIE_NAME = "ptk";
    private static final String PTK_PREFIX = "portal:ptk:";
    private static final List<ScopeEntry> SCOPE_ENTRIES = Arrays.asList(
            new ScopeEntry("POST", "/portal/api/password/change", "PWD_CHANGE"),
            new ScopeEntry("GET", "/portal/api/profile", "PROFILE_EDIT"),
            new ScopeEntry("POST", "/portal/api/profile", "PROFILE_EDIT")
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PtkInterceptor(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        ScopeEntry scopeEntry = matchScopeEntry(request);
        if (scopeEntry == null) {
            return true;
        }
        if (RequestContext.getUserId() != null) {
            return true;
        }
        String ptk = resolvePtk(request);
        if (!StringUtils.hasText(ptk)) {
            writeFailure(response, ErrorCode.PTK_INVALID);
            return false;
        }
        String payload = stringRedisTemplate.opsForValue().get(buildPtkKey(ptk));
        if (!StringUtils.hasText(payload)) {
            writeFailure(response, ErrorCode.PTK_EXPIRED);
            return false;
        }
        PtkPayload ptkPayload = parsePayload(payload);
        if (ptkPayload == null || !StringUtils.hasText(ptkPayload.getScope())) {
            writeFailure(response, ErrorCode.PTK_INVALID);
            return false;
        }
        if (!scopeEntry.matchesScope(ptkPayload.getScope())) {
            writeFailure(response, ErrorCode.PTK_SCOPE_MISMATCH);
            return false;
        }
        if (ptkPayload.getUserId() != null) {
            RequestContext.setUserId(ptkPayload.getUserId());
            request.setAttribute("userId", ptkPayload.getUserId());
        }
        return true;
    }

    private ScopeEntry matchScopeEntry(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return SCOPE_ENTRIES.stream().filter(entry -> entry.matches(method, path)).findFirst().orElse(null);
    }

    private String resolvePtk(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        Optional<Cookie> cookie = Arrays.stream(cookies)
                .filter(item -> PTK_COOKIE_NAME.equals(item.getName()))
                .findFirst();
        return cookie.map(Cookie::getValue).orElse(null);
    }

    private PtkPayload parsePayload(String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            return PtkPayload.from(data);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void writeFailure(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(errorCode));
    }

    private String buildPtkKey(String ptk) {
        return PTK_PREFIX + ptk;
    }

    private static class ScopeEntry {
        private final String method;
        private final String path;
        private final String scope;

        private ScopeEntry(String method, String path, String scope) {
            this.method = method;
            this.path = path;
            this.scope = scope;
        }

        private boolean matches(String method, String path) {
            return this.method.equalsIgnoreCase(method) && this.path.equalsIgnoreCase(path);
        }

        private boolean matchesScope(String scope) {
            return this.scope.equalsIgnoreCase(scope);
        }
    }

    private static class PtkPayload {
        private final Long userId;
        private final String scope;

        private PtkPayload(Long userId, String scope) {
            this.userId = userId;
            this.scope = scope;
        }

        public static PtkPayload from(Map<String, Object> data) {
            if (data == null || data.isEmpty()) {
                return null;
            }
            Long userId = parseLong(data.get("userId"));
            String scope = data.get("scope") == null ? null : data.get("scope").toString();
            return new PtkPayload(userId, scope);
        }

        public Long getUserId() {
            return userId;
        }

        public String getScope() {
            return scope;
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
    }
}
