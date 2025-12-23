package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/portal/api/admin/app-menus")
@Validated
@AdminRequired
/**
 * AdminAppMenuController。
 */
public class AdminAppMenuController {

    private final PortalAdminAppMenuService portalAdminAppMenuService;

    public AdminAppMenuController(PortalAdminAppMenuService portalAdminAppMenuService) {
        this.portalAdminAppMenuService = portalAdminAppMenuService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<AppMenuTreeNode>> tree(@RequestParam(required = false) String appCode) {
        PortalAdminAppMenuService.Result<List<AppMenuTreeNode>> result =
                portalAdminAppMenuService.loadMenuTree(appCode);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(result.getData());
    }

    @PostMapping
    public ApiResponse<ActionResponse> create(@Valid @RequestBody MenuCreateRequest request,
                                              HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
        AppMenuResource menu = request.toEntity();
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.createMenu(
                menu,
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActionResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody MenuUpdateRequest request,
                                              HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.updateMenu(
                id,
                request.toEntity(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ActionResponse> delete(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.deleteMenu(
                id,
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<ActionResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusRequest request,
                                                    HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.updateStatus(
                id,
                request.getStatus(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static class MenuCreateRequest {
        @NotBlank(message = "appCode 不能为空")
        private String appCode;
        @NotBlank(message = "menuCode 不能为空")
        private String menuCode;
        @NotBlank(message = "menuName 不能为空")
        private String menuName;
        private String menuPath;
        private String menuType;
        private Long parentId;
        private String permission;
        private Integer sort;
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

    public static class MenuUpdateRequest {
        private String appCode;
        private String menuCode;
        private String menuName;
        private String menuPath;
        private String menuType;
        private Long parentId;
        private String permission;
        private Integer sort;
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

    public static class StatusRequest {
        @NotNull(message = "status 不能为空")
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    public static class ActionResponse {
        private final boolean success;

        public ActionResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
