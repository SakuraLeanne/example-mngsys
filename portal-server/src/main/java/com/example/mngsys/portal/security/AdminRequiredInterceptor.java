package com.example.mngsys.portal.security;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
/**
 * AdminRequiredInterceptor。
 */
public class AdminRequiredInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AdminRequiredInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (!requiresAdmin(handlerMethod)) {
            return true;
        }
        String userId = RequestContext.getUserId();
        if (userId != null && ("1".equals(userId) || "u-admin-0001".equals(userId))) {
            return true;
        }
        writeForbidden(response);
        return false;
    }

    private boolean requiresAdmin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(AdminRequired.class)
                || handlerMethod.getBeanType().isAnnotationPresent(AdminRequired.class);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.FORBIDDEN));
    }
}
