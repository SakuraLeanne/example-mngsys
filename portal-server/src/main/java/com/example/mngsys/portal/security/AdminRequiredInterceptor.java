package com.example.mngsys.portal.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.entity.AppUserRole;
import com.example.mngsys.portal.service.AppRoleService;
import com.example.mngsys.portal.service.AppUserRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
/**
 * AdminRequiredInterceptor。
 */
public class AdminRequiredInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final ObjectMapper objectMapper;
    private final AppUserRoleService appUserRoleService;
    private final AppRoleService appRoleService;

    public AdminRequiredInterceptor(ObjectMapper objectMapper,
                                    AppUserRoleService appUserRoleService,
                                    AppRoleService appRoleService) {
        this.objectMapper = objectMapper;
        this.appUserRoleService = appUserRoleService;
        this.appRoleService = appRoleService;
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
        if (StringUtils.hasText(userId) && isAdmin(userId)) {
            return true;
        }
        writeForbidden(response);
        return false;
    }

    private boolean requiresAdmin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(AdminRequired.class)
                || handlerMethod.getBeanType().isAnnotationPresent(AdminRequired.class);
    }

    private boolean isAdmin(String userId) {
        List<AppUserRole> relations = appUserRoleService.list(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId));
        if (relations == null || relations.isEmpty()) {
            return false;
        }
        List<Long> roleIds = relations.stream()
                .map(AppUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        return appRoleService.list(new LambdaQueryWrapper<AppRole>()
                .in(AppRole::getId, roleIds)
                .eq(AppRole::getStatus, 1))
                .stream()
                .anyMatch(role -> ADMIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode()));
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.FORBIDDEN));
    }
}
