package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.entity.AppMenuResource;
import com.dhgx.portal.entity.AppRoleMenu;
import com.dhgx.portal.entity.AppRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * PortalAdminAppRoleService。
 */
public class PortalAdminAppRoleService {

    private final AppRoleService appRoleService;
    private final AppRoleMenuService appRoleMenuService;
    private final AppMenuResourceService appMenuResourceService;

    public PortalAdminAppRoleService(AppRoleService appRoleService,
                                     AppRoleMenuService appRoleMenuService,
                                     AppMenuResourceService appMenuResourceService) {
        this.appRoleService = appRoleService;
        this.appRoleMenuService = appRoleMenuService;
        this.appMenuResourceService = appMenuResourceService;
    }

    public Result<List<AppRole>> listRoles(String appCode, Integer status) {
        LambdaQueryWrapper<AppRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(appCode)) {
            wrapper.eq(AppRole::getAppCode, appCode);
        }
        if (status != null) {
            wrapper.eq(AppRole::getStatus, status);
        }
        wrapper.orderByDesc(AppRole::getId);
        return Result.success(appRoleService.list(wrapper));
    }

    @Transactional
    public Result<AppRole> createRole(AppRole role) {
        if (role == null || !StringUtils.hasText(role.getAppCode())
                || !StringUtils.hasText(role.getRoleCode())
                || !StringUtils.hasText(role.getRoleName())) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色信息不完整");
        }
        if (existsRoleCode(role.getAppCode(), role.getRoleCode(), null)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色编码已存在");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        appRoleService.save(role);
        return Result.success(role);
    }

    @Transactional
    public Result<AppRole> updateRole(Long id, AppRole update) {
        if (id == null || update == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色信息不完整");
        }
        AppRole role = appRoleService.getById(id);
        if (role == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "角色不存在");
        }
        String appCode = StringUtils.hasText(update.getAppCode()) ? update.getAppCode() : role.getAppCode();
        if (StringUtils.hasText(update.getRoleCode())
                && existsRoleCode(appCode, update.getRoleCode(), id)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色编码已存在");
        }
        role.setAppCode(appCode);
        if (StringUtils.hasText(update.getRoleCode())) {
            role.setRoleCode(update.getRoleCode());
        }
        if (StringUtils.hasText(update.getRoleName())) {
            role.setRoleName(update.getRoleName());
        }
        if (update.getRemark() != null) {
            role.setRemark(update.getRemark());
        }
        if (update.getStatus() != null) {
            role.setStatus(update.getStatus());
        }
        appRoleService.updateById(role);
        return Result.success(role);
    }

    @Transactional
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "状态不能为空");
        }
        AppRole role = appRoleService.getById(id);
        if (role == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "角色不存在");
        }
        role.setStatus(status);
        appRoleService.updateById(role);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> grantMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色ID不能为空");
        }
        AppRole role = appRoleService.getById(roleId);
        if (role == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "角色不存在");
        }
        List<Long> normalized = menuIds == null ? new ArrayList<>() : menuIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(normalized)) {
            List<AppMenuResource> menus = appMenuResourceService.list(new LambdaQueryWrapper<AppMenuResource>()
                    .in(AppMenuResource::getId, normalized));
            if (menus.size() != normalized.size()) {
                return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单不存在");
            }
            Set<String> appCodes = menus.stream().map(AppMenuResource::getAppCode).collect(Collectors.toSet());
            if (appCodes.size() > 1 || !appCodes.contains(role.getAppCode())) {
                return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单应用不匹配");
            }
        }
        appRoleMenuService.remove(new LambdaQueryWrapper<AppRoleMenu>()
                .eq(AppRoleMenu::getRoleId, roleId));
        if (!CollectionUtils.isEmpty(normalized)) {
            List<AppRoleMenu> relations = normalized.stream().map(menuId -> {
                AppRoleMenu relation = new AppRoleMenu();
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                relation.setCreateTime(LocalDateTime.now());
                return relation;
            }).collect(Collectors.toList());
            appRoleMenuService.saveBatch(relations);
        }
        return Result.success(null);
    }

    public Result<RoleMenuAuthorization> listRoleMenuAuthorization(Long roleId) {
        if (roleId == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色ID不能为空");
        }
        AppRole role = appRoleService.getById(roleId);
        if (role == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "角色不存在");
        }
        List<AppMenuResource> menus = appMenuResourceService.list(new LambdaQueryWrapper<AppMenuResource>()
                .eq(AppMenuResource::getAppCode, role.getAppCode()));
        List<AppRoleMenu> roleMenus = appRoleMenuService.list(new LambdaQueryWrapper<AppRoleMenu>()
                .eq(AppRoleMenu::getRoleId, roleId));
        Set<Long> grantedMenuIds = roleMenus.stream()
                .map(AppRoleMenu::getMenuId)
                .collect(Collectors.toSet());
        List<AppMenuResource> grantedMenus = new ArrayList<>();
        List<AppMenuResource> ungrantedMenus = new ArrayList<>();
        for (AppMenuResource menu : menus) {
            if (grantedMenuIds.contains(menu.getId())) {
                grantedMenus.add(menu);
            } else {
                ungrantedMenus.add(menu);
            }
        }
        return Result.success(new RoleMenuAuthorization(grantedMenus, ungrantedMenus));
    }

    private boolean existsRoleCode(String appCode, String roleCode, Long excludeId) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(roleCode)) {
            return false;
        }
        LambdaQueryWrapper<AppRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppRole::getAppCode, appCode)
                .eq(AppRole::getRoleCode, roleCode);
        if (excludeId != null) {
            wrapper.ne(AppRole::getId, excludeId);
        }
        return appRoleService.count(wrapper) > 0;
    }

    public static class Result<T> {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String message;
        private final T data;

        private Result(boolean success, ErrorCode errorCode, String message, T data) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
            this.data = data;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(true, null, null, data);
        }

        public static <T> Result<T> failure(ErrorCode errorCode, String message) {
            return new Result<>(false, errorCode, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }

    public static class RoleMenuAuthorization {
        private final List<AppMenuResource> grantedMenus;
        private final List<AppMenuResource> ungrantedMenus;

        public RoleMenuAuthorization(List<AppMenuResource> grantedMenus, List<AppMenuResource> ungrantedMenus) {
            this.grantedMenus = grantedMenus;
            this.ungrantedMenus = ungrantedMenus;
        }

        public List<AppMenuResource> getGrantedMenus() {
            return grantedMenus;
        }

        public List<AppMenuResource> getUngrantedMenus() {
            return ungrantedMenus;
        }
    }
}
