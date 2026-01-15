package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.controller.dto.AppMenuTreeNode;
import com.example.mngsys.portal.entity.AppMenuResource;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminAppMenuService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 管理端应用菜单控制器，提供菜单树查询与增删改接口。
 */
@RestController
@RequestMapping("/admin/app-menus")
@Validated
@AdminRequired
public class AdminAppMenuController {

    /**
     * 应用菜单管理服务。
     */
    private final PortalAdminAppMenuService portalAdminAppMenuService;

    /**
     * 构造函数，注入菜单管理服务。
     *
     * @param portalAdminAppMenuService 菜单管理服务
     */
    public AdminAppMenuController(PortalAdminAppMenuService portalAdminAppMenuService) {
        this.portalAdminAppMenuService = portalAdminAppMenuService;
    }

    /**
     * 查询菜单树。
     *
     * @param appCode 应用编码，可选
     * @return 菜单树结构
     */
    @GetMapping("/tree")
    public ApiResponse<List<AppMenuTreeNode>> tree(@RequestParam(required = false) String appCode) {
        PortalAdminAppMenuService.Result<List<AppMenuTreeNode>> result =
                portalAdminAppMenuService.loadMenuTree(appCode);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(result.getData());
    }

    /**
     * 创建菜单。
     *
     * @param request 创建请求
     * @return 操作结果
     */
    @PostMapping
    public ApiResponse<ActionResponse> create(@Valid @RequestBody MenuCreateRequest request) {
        AppMenuResource menu = request.toEntity();
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.createMenu(
                menu);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 更新菜单。
     *
     * @param id      菜单 ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ApiResponse<ActionResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody MenuUpdateRequest request) {
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.updateMenu(
                id,
                request.toEntity());
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 删除菜单。
     *
     * @param id 菜单 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<ActionResponse> delete(@PathVariable Long id) {
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.deleteMenu(
                id);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 更新菜单状态。
     *
     * @param id      菜单 ID
     * @param request 状态请求
     * @return 操作结果
     */
    @PostMapping("/{id}/status")
    public ApiResponse<ActionResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusRequest request) {
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.updateStatus(
                id,
                request.getStatus());
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 菜单创建请求体。
     */
    public static class MenuCreateRequest {
        /**
         * 应用编码。
         */
        @NotBlank(message = "appCode 不能为空")
        private String appCode;
        /**
         * 菜单编码。
         */
        @NotBlank(message = "menuCode 不能为空")
        private String menuCode;
        /**
         * 菜单名称。
         */
        @NotBlank(message = "menuName 不能为空")
        private String menuName;
        /**
         * 菜单路由路径。
         */
        private String menuPath;
        /**
         * 菜单类型。
         */
        private String menuType;
        /**
         * 父级菜单 ID。
         */
        private Long parentId;
        /**
         * 权限标识。
         */
        private String permission;
        /**
         * 排序。
         */
        private Integer sort;
        /**
         * 状态。
         */
        private Integer status;

        public String getAppCode() {
            return appCode;
        }

        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }

        public String getMenuCode() {
            return menuCode;
        }

        public void setMenuCode(String menuCode) {
            this.menuCode = menuCode;
        }

        public String getMenuName() {
            return menuName;
        }

        public void setMenuName(String menuName) {
            this.menuName = menuName;
        }

        public String getMenuPath() {
            return menuPath;
        }

        public void setMenuPath(String menuPath) {
            this.menuPath = menuPath;
        }

        public String getMenuType() {
            return menuType;
        }

        public void setMenuType(String menuType) {
            this.menuType = menuType;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public AppMenuResource toEntity() {
            AppMenuResource menu = new AppMenuResource();
            menu.setAppCode(appCode);
            menu.setMenuCode(menuCode);
            menu.setMenuName(menuName);
            menu.setMenuPath(menuPath);
            menu.setMenuType(menuType);
            menu.setParentId(parentId);
            menu.setPermission(permission);
            menu.setSort(sort == null ? 0 : sort);
            menu.setStatus(status == null ? 1 : status);
            return menu;
        }
    }

    /**
     * 菜单更新请求体。
     */
    public static class MenuUpdateRequest {
        /**
         * 应用编码。
         */
        private String appCode;
        /**
         * 菜单编码。
         */
        private String menuCode;
        /**
         * 菜单名称。
         */
        private String menuName;
        /**
         * 菜单路由路径。
         */
        private String menuPath;
        /**
         * 菜单类型。
         */
        private String menuType;
        /**
         * 父级菜单 ID。
         */
        private Long parentId;
        /**
         * 权限标识。
         */
        private String permission;
        /**
         * 排序。
         */
        private Integer sort;
        /**
         * 状态。
         */
        private Integer status;

        public String getAppCode() {
            return appCode;
        }

        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }

        public String getMenuCode() {
            return menuCode;
        }

        public void setMenuCode(String menuCode) {
            this.menuCode = menuCode;
        }

        public String getMenuName() {
            return menuName;
        }

        public void setMenuName(String menuName) {
            this.menuName = menuName;
        }

        public String getMenuPath() {
            return menuPath;
        }

        public void setMenuPath(String menuPath) {
            this.menuPath = menuPath;
        }

        public String getMenuType() {
            return menuType;
        }

        public void setMenuType(String menuType) {
            this.menuType = menuType;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public AppMenuResource toEntity() {
            AppMenuResource menu = new AppMenuResource();
            menu.setAppCode(appCode);
            menu.setMenuCode(menuCode);
            menu.setMenuName(menuName);
            menu.setMenuPath(menuPath);
            menu.setMenuType(menuType);
            menu.setParentId(parentId);
            menu.setPermission(permission);
            menu.setSort(sort);
            menu.setStatus(status);
            return menu;
        }
    }

    /**
     * 菜单状态更新请求体。
     */
    public static class StatusRequest {
        /**
         * 状态值。
         */
        @NotNull(message = "status 不能为空")
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 通用操作结果。
     */
    public static class ActionResponse {
        /**
         * 是否成功。
         */
        private final boolean success;

        public ActionResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
