package com.dhgx.portal.security;

import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.context.RequestContext;
import com.dhgx.portal.service.RolePermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    private final RolePermissionService rolePermissionService;

    public AdminRequiredInterceptor(ObjectMapper objectMapper,
                                    RolePermissionService rolePermissionService) {
        this.objectMapper = objectMapper;
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        AdminRequired adminRequired = resolveAdminRequired(handlerMethod);
        if (adminRequired == null) {
            return true;
        }
        String userId = RequestContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            writeForbidden(response, "登录已失效，请重新登录");
            return false;
        }
        if (isAuthorized(userId, adminRequired, request)) {
            return true;
        }
        writeForbidden(response, "权限不足，请联系管理员");
        return false;
    }

    private AdminRequired resolveAdminRequired(HandlerMethod handlerMethod) {
        AdminRequired methodAnnotation = handlerMethod.getMethodAnnotation(AdminRequired.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(AdminRequired.class);
    }

    private boolean isAuthorized(String userId, AdminRequired adminRequired, HttpServletRequest request) {
        if ("portal".equalsIgnoreCase(adminRequired.scope())) {
            return rolePermissionService.isPortalAdmin(userId);
        }
        if (!"app".equalsIgnoreCase(adminRequired.scope())) {
            return false;
        }
        String appCode = request.getParameter(adminRequired.appCodeParam());
        if (StringUtils.hasText(appCode)) {
            return rolePermissionService.isAppAdmin(userId, appCode);
        }
        return adminRequired.allowAnyAppAdmin() && rolePermissionService.isAnyAppAdmin(userId);
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.FORBIDDEN, message));
    }
}
