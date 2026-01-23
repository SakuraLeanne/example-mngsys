package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.controller.dto.AppMenuTreeNode;
import com.dhgx.portal.entity.AppMenuResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * PortalAdminAppMenuService。
 */
public class PortalAdminAppMenuService {

    private final AppMenuResourceService appMenuResourceService;
    private final RolePermissionService rolePermissionService;
    private final AppMenuDeliveryService appMenuDeliveryService;

    public PortalAdminAppMenuService(AppMenuResourceService appMenuResourceService,
                                     RolePermissionService rolePermissionService,
                                     AppMenuDeliveryService appMenuDeliveryService) {
        this.appMenuResourceService = appMenuResourceService;
        this.rolePermissionService = rolePermissionService;
        this.appMenuDeliveryService = appMenuDeliveryService;
    }

    public Result<List<AppMenuTreeNode>> loadMenuTree(String appCode, String operatorId) {
        String resolvedAppCode = appCode;
        if (!rolePermissionService.isAppAdmin(operatorId, appCode)) {
            List<AppMenuTreeNode> menus = appMenuDeliveryService.loadMenus(operatorId);
            if (!StringUtils.hasText(appCode)) {
                return Result.success(menus);
            }
            return Result.success(filterMenusByAppCode(menus, appCode));
        }
        if (!StringUtils.hasText(appCode)) {
            Set<String> appCodes = rolePermissionService.listAppAdminAppCodes(operatorId);
            if (appCodes.size() > 1) {
                return Result.failure(ErrorCode.INVALID_ARGUMENT, "请指定应用编码");
            }
            if (appCodes.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            resolvedAppCode = appCodes.iterator().next();
        }
        LambdaQueryWrapper<AppMenuResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppMenuResource::getAppCode, resolvedAppCode);
        wrapper.orderByAsc(AppMenuResource::getSort).orderByAsc(AppMenuResource::getId);
        List<AppMenuResource> menus = appMenuResourceService.list(wrapper);
        return Result.success(buildTree(menus));
    }

    @Transactional
    public Result<Void> createMenu(AppMenuResource menu, String operatorId) {
        if (menu == null || !StringUtils.hasText(menu.getAppCode())
                || !StringUtils.hasText(menu.getMenuCode())
                || !StringUtils.hasText(menu.getMenuName())) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单信息不完整");
        }
        if (!rolePermissionService.isAppAdmin(operatorId, menu.getAppCode())) {
            return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
        }
        if (StringUtils.hasText(menu.getMenuPath()) && existsMenuPath(menu.getAppCode(), menu.getMenuPath(), null)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单路径已存在");
        }
        if (existsMenuCode(menu.getAppCode(), menu.getMenuCode(), null)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单编码已存在");
        }
        appMenuResourceService.save(menu);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> updateMenu(Long id, AppMenuResource update, String operatorId) {
        if (id == null || update == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单信息不完整");
        }
        AppMenuResource menu = appMenuResourceService.getById(id);
        if (menu == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        String appCode = StringUtils.hasText(update.getAppCode()) ? update.getAppCode() : menu.getAppCode();
        if (!rolePermissionService.isAppAdmin(operatorId, appCode)) {
            return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
        }
        String menuPath = update.getMenuPath();
        if (StringUtils.hasText(menuPath) && existsMenuPath(appCode, menuPath, id)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单路径已存在");
        }
        String menuCode = update.getMenuCode();
        if (StringUtils.hasText(menuCode) && existsMenuCode(appCode, menuCode, id)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单编码已存在");
        }
        menu.setAppCode(appCode);
        if (StringUtils.hasText(update.getMenuCode())) {
            menu.setMenuCode(update.getMenuCode());
        }
        if (StringUtils.hasText(update.getMenuName())) {
            menu.setMenuName(update.getMenuName());
        }
        if (update.getMenuPath() != null) {
            menu.setMenuPath(update.getMenuPath());
        }
        if (update.getMenuType() != null) {
            menu.setMenuType(update.getMenuType());
        }
        if (update.getParentId() != null) {
            menu.setParentId(update.getParentId());
        }
        if (update.getPermission() != null) {
            menu.setPermission(update.getPermission());
        }
        if (update.getSort() != null) {
            menu.setSort(update.getSort());
        }
        if (update.getStatus() != null) {
            menu.setStatus(update.getStatus());
        }
        appMenuResourceService.updateById(menu);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> updateStatus(Long id, Integer status, String operatorId) {
        if (id == null || status == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "状态不能为空");
        }
        AppMenuResource menu = appMenuResourceService.getById(id);
        if (menu == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        if (!rolePermissionService.isAppAdmin(operatorId, menu.getAppCode())) {
            return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
        }
        menu.setStatus(status);
        appMenuResourceService.updateById(menu);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> deleteMenus(List<Long> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单ID不能为空");
        }
        List<Long> uniqueIds = ids.stream().distinct().collect(Collectors.toList());
        for (Long id : uniqueIds) {
            if (id == null) {
                return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单ID不能为空");
            }
            AppMenuResource menu = appMenuResourceService.getById(id);
            if (menu == null) {
                return Result.failure(ErrorCode.NOT_FOUND, "菜单不存在");
            }
            if (!rolePermissionService.isAppAdmin(operatorId, menu.getAppCode())) {
                return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
            }
            boolean hasChild = appMenuResourceService.count(new LambdaQueryWrapper<AppMenuResource>()
                    .eq(AppMenuResource::getParentId, id)) > 0;
            if (hasChild) {
                return Result.failure(ErrorCode.INVALID_ARGUMENT, "存在子菜单，无法删除");
            }
        }
        appMenuResourceService.removeByIds(uniqueIds);
        return Result.success(null);
    }


    private boolean existsMenuPath(String appCode, String menuPath, Long excludeId) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(menuPath)) {
            return false;
        }
        LambdaQueryWrapper<AppMenuResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppMenuResource::getAppCode, appCode)
                .eq(AppMenuResource::getMenuPath, menuPath);
        if (excludeId != null) {
            wrapper.ne(AppMenuResource::getId, excludeId);
        }
        return appMenuResourceService.count(wrapper) > 0;
    }

    private boolean existsMenuCode(String appCode, String menuCode, Long excludeId) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(menuCode)) {
            return false;
        }
        LambdaQueryWrapper<AppMenuResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppMenuResource::getAppCode, appCode)
                .eq(AppMenuResource::getMenuCode, menuCode);
        if (excludeId != null) {
            wrapper.ne(AppMenuResource::getId, excludeId);
        }
        return appMenuResourceService.count(wrapper) > 0;
    }

    private List<AppMenuTreeNode> buildTree(List<AppMenuResource> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, AppMenuTreeNode> nodeMap = new HashMap<>();
        for (AppMenuResource menu : menus) {
            nodeMap.put(menu.getId(), toNode(menu));
        }
        List<AppMenuTreeNode> roots = new ArrayList<>();
        for (AppMenuResource menu : menus) {
            AppMenuTreeNode node = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private AppMenuTreeNode toNode(AppMenuResource menu) {
        return new AppMenuTreeNode(menu.getId(), menu.getAppCode(), menu.getMenuCode(), menu.getMenuName(),
                menu.getMenuPath(), menu.getMenuType(), menu.getParentId(), menu.getPermission(), menu.getSort(),
                menu.getStatus());
    }

    private void sortTree(List<AppMenuTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Comparator<AppMenuTreeNode> comparator = Comparator
                .comparing((AppMenuTreeNode node) -> node.getSort() == null ? 0 : node.getSort())
                .thenComparing(node -> node.getId() == null ? 0L : node.getId());
        nodes.sort(comparator);
        for (AppMenuTreeNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                node.getChildren().sort(comparator);
                sortTree(node.getChildren());
            }
        }
    }

    private List<AppMenuTreeNode> filterMenusByAppCode(List<AppMenuTreeNode> menus, String appCode) {
        if (menus == null || menus.isEmpty() || !StringUtils.hasText(appCode)) {
            return new ArrayList<>();
        }
        List<AppMenuTreeNode> filtered = new ArrayList<>();
        for (AppMenuTreeNode node : menus) {
            List<AppMenuTreeNode> children = filterMenusByAppCode(node.getChildren(), appCode);
            if (appCode.equalsIgnoreCase(node.getAppCode()) || !children.isEmpty()) {
                node.getChildren().clear();
                node.getChildren().addAll(children);
                filtered.add(node);
            }
        }
        return filtered;
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
}
