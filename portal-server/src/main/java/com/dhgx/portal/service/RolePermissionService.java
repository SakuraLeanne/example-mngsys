package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.portal.entity.AppRole;
import com.dhgx.portal.entity.AppUserRole;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {

    public static final String PORTAL_APP_CODE = "portal";
    public static final String PORTAL_ADMIN_ROLE = "portal_admin";
    public static final String APP_ADMIN_ROLE = "app_admin";

    private final AppUserRoleService appUserRoleService;
    private final AppRoleService appRoleService;

    public RolePermissionService(AppUserRoleService appUserRoleService, AppRoleService appRoleService) {
        this.appUserRoleService = appUserRoleService;
        this.appRoleService = appRoleService;
    }

    public boolean isPortalAdmin(String userId) {
        return hasRole(userId, PORTAL_APP_CODE, PORTAL_ADMIN_ROLE);
    }

    public boolean isAppAdmin(String userId, String appCode) {
        if (!StringUtils.hasText(appCode)) {
            return false;
        }
        return hasRole(userId, appCode, APP_ADMIN_ROLE);
    }

    public boolean isAnyAppAdmin(String userId) {
        return listActiveRoles(userId).stream()
                .anyMatch(role -> APP_ADMIN_ROLE.equalsIgnoreCase(role.getRoleCode()));
    }

    public Set<String> listAppAdminAppCodes(String userId) {
        return listActiveRoles(userId).stream()
                .filter(role -> APP_ADMIN_ROLE.equalsIgnoreCase(role.getRoleCode()))
                .map(AppRole::getAppCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public List<AppRole> listActiveRoles(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }
        List<AppUserRole> relations = appUserRoleService.list(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId));
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream()
                .map(AppUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return appRoleService.list(new LambdaQueryWrapper<AppRole>()
                .in(AppRole::getId, roleIds)
                .eq(AppRole::getStatus, 1));
    }

    public Set<Long> listUserRoleIds(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptySet();
        }
        List<AppUserRole> relations = appUserRoleService.list(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId));
        if (relations == null || relations.isEmpty()) {
            return Collections.emptySet();
        }
        return relations.stream()
                .map(AppUserRole::getRoleId)
                .collect(Collectors.toSet());
    }

    public boolean hasRoleId(String userId, Long roleId) {
        if (roleId == null) {
            return false;
        }
        return listUserRoleIds(userId).contains(roleId);
    }

    private boolean hasRole(String userId, String appCode, String roleCode) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(appCode) || !StringUtils.hasText(roleCode)) {
            return false;
        }
        return listActiveRoles(userId).stream()
                .anyMatch(role -> appCode.equalsIgnoreCase(role.getAppCode())
                        && roleCode.equalsIgnoreCase(role.getRoleCode()));
    }
}
