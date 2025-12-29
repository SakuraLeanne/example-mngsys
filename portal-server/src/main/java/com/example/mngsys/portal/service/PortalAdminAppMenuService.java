package com.example.mngsys.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.controller.dto.AppMenuTreeNode;
import com.example.mngsys.portal.entity.AppMenuResource;
import com.example.mngsys.portal.entity.PortalAuditLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * PortalAdminAppMenuService。
 */
public class PortalAdminAppMenuService {

    private final AppMenuResourceService appMenuResourceService;
    private final PortalAuditLogService portalAuditLogService;

    public PortalAdminAppMenuService(AppMenuResourceService appMenuResourceService,
                                     PortalAuditLogService portalAuditLogService) {
        this.appMenuResourceService = appMenuResourceService;
        this.portalAuditLogService = portalAuditLogService;
    }

    public Result<List<AppMenuTreeNode>> loadMenuTree(String appCode) {
        LambdaQueryWrapper<AppMenuResource> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(appCode)) {
            wrapper.eq(AppMenuResource::getAppCode, appCode);
        }
        wrapper.orderByAsc(AppMenuResource::getSort).orderByAsc(AppMenuResource::getId);
        List<AppMenuResource> menus = appMenuResourceService.list(wrapper);
        return Result.success(buildTree(menus));
    }

    @Transactional
    public Result<Void> createMenu(AppMenuResource menu, String operatorId, String ip) {
        if (menu == null || !StringUtils.hasText(menu.getAppCode())
                || !StringUtils.hasText(menu.getMenuCode())
                || !StringUtils.hasText(menu.getMenuName())) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单信息不完整");
        }
        if (StringUtils.hasText(menu.getMenuPath()) && existsMenuPath(menu.getAppCode(), menu.getMenuPath(), null)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单路径已存在");
        }
        if (existsMenuCode(menu.getAppCode(), menu.getMenuCode(), null)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单编码已存在");
        }
        appMenuResourceService.save(menu);
        writeAuditLog(operatorId, "CREATE_APP_MENU", String.valueOf(menu.getId()), menu.getMenuName(), ip);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> updateMenu(Long id, AppMenuResource update, String operatorId, String ip) {
        if (id == null || update == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单信息不完整");
        }
        AppMenuResource menu = appMenuResourceService.getById(id);
        if (menu == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        String appCode = StringUtils.hasText(update.getAppCode()) ? update.getAppCode() : menu.getAppCode();
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
        writeAuditLog(operatorId, "UPDATE_APP_MENU", String.valueOf(menu.getId()), menu.getMenuName(), ip);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> updateStatus(Long id, Integer status, String operatorId, String ip) {
        if (id == null || status == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "状态不能为空");
        }
        AppMenuResource menu = appMenuResourceService.getById(id);
        if (menu == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        menu.setStatus(status);
        appMenuResourceService.updateById(menu);
        writeAuditLog(operatorId, "UPDATE_APP_MENU_STATUS", String.valueOf(menu.getId()),
                String.valueOf(status), ip);
        return Result.success(null);
    }

    @Transactional
    public Result<Void> deleteMenu(Long id, String operatorId, String ip) {
        if (id == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "菜单ID不能为空");
        }
        AppMenuResource menu = appMenuResourceService.getById(id);
        if (menu == null) {
            return Result.failure(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        boolean hasChild = appMenuResourceService.count(new LambdaQueryWrapper<AppMenuResource>()
                .eq(AppMenuResource::getParentId, id)) > 0;
        if (hasChild) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "存在子菜单，无法删除");
        }
        appMenuResourceService.removeById(id);
        writeAuditLog(operatorId, "DELETE_APP_MENU", String.valueOf(id), menu.getMenuName(), ip);
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

    private void writeAuditLog(String operatorId, String action, String resource, String detail, String ip) {
        PortalAuditLog log = new PortalAuditLog();
        log.setUserId(operatorId);
        log.setAction(action);
        log.setResource(resource);
        log.setDetail(detail);
        log.setIp(ip);
        log.setStatus(1);
        log.setCreateTime(LocalDateTime.now());
        portalAuditLogService.save(log);
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
