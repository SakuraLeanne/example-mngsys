package com.example.mngsys.portal.controller;

import com.example.mngsys.common.api.ActionResponse;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminAppRoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端应用角色控制器，提供角色查询、创建、修改、授权菜单等接口。
 */
@RestController
@RequestMapping("/admin/app-roles")
@Validated
@AdminRequired
public class AdminAppRoleController {

    /**
     * 应用角色管理服务。
     */
    private final PortalAdminAppRoleService portalAdminAppRoleService;

    /**
     * 构造函数，注入角色管理服务。
     *
     * @param portalAdminAppRoleService 角色管理服务
     */
    public AdminAppRoleController(PortalAdminAppRoleService portalAdminAppRoleService) {
        this.portalAdminAppRoleService = portalAdminAppRoleService;
    }

    /**
     * 查询角色列表。
     *
     * @param appCode 应用编码，可选
     * @param status  状态，可选
     * @return 角色概要列表
     */
    @GetMapping
    public ApiResponse<List<RoleSummary>> list(@RequestParam(required = false) String appCode,
                                               @RequestParam(required = false) Integer status) {
        PortalAdminAppRoleService.Result<List<AppRole>> result = portalAdminAppRoleService.listRoles(appCode, status);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        List<RoleSummary> roles = result.getData().stream()
                .map(RoleSummary::from)
                .collect(Collectors.toList());
        return ApiResponse.success(roles);
    }

    /**
     * 创建或更新角色。
     *
     * @param request 角色信息
     * @return 角色概要
     */
    @PostMapping
    public ApiResponse<RoleSummary> save(@Valid @RequestBody RoleSummary request) {
        PortalAdminAppRoleService.Result<AppRole> result;
        if (request.getId() == null) {
            result = portalAdminAppRoleService.createRole(request.toEntity());
        } else {
            result = portalAdminAppRoleService.updateRole(request.getId(), request.toEntity());
        }
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(RoleSummary.from(result.getData()));
    }

    /**
     * 更新角色状态。
     *
     * @param id      角色 ID
     * @param status  状态值
     * @return 操作结果
     */
    @GetMapping("/status")
    public ApiResponse<ActionResponse> updateStatus(@RequestParam @NotNull(message = "id 不能为空") Long id,
                                                    @RequestParam
                                                    @NotNull(message = "status 不能为空") Integer status) {
        PortalAdminAppRoleService.Result<Void> result = portalAdminAppRoleService.updateStatus(
                id,
                status);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 为角色授权菜单。
     *
     * @param id      角色 ID
     * @param request 菜单授权请求
     * @return 操作结果
     */
    @PostMapping("/{id}/grant-menus")
    public ApiResponse<ActionResponse> grantMenus(@PathVariable Long id,
                                                  @Valid @RequestBody GrantMenusRequest request) {
        PortalAdminAppRoleService.Result<Void> result = portalAdminAppRoleService.grantMenus(
                id,
                request.getMenuIds());
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 角色概要信息。
     */
    public static class RoleSummary {
        /**
         * 角色 ID。
         */
        private Long id;
        /**
         * 应用编码。
         */
        @NotBlank(message = "appCode 不能为空")
        private String appCode;
        /**
         * 角色编码。
         */
        @NotBlank(message = "roleCode 不能为空")
        private String roleCode;
        /**
         * 角色名称。
         */
        @NotBlank(message = "roleName 不能为空")
        private String roleName;
        /**
         * 状态。
         */
        private Integer status;
        /**
         * 备注。
         */
        private String remark;
        /**
         * 创建时间。
         */
        private LocalDateTime createTime;
        /**
         * 更新时间。
         */
        private LocalDateTime updateTime;

        public RoleSummary() {
        }

        public RoleSummary(Long id, String appCode, String roleCode, String roleName, Integer status,
                           String remark, LocalDateTime createTime, LocalDateTime updateTime) {
            this.id = id;
            this.appCode = appCode;
            this.roleCode = roleCode;
            this.roleName = roleName;
            this.status = status;
            this.remark = remark;
            this.createTime = createTime;
            this.updateTime = updateTime;
        }

        public static RoleSummary from(AppRole role) {
            if (role == null) {
                return null;
            }
            return new RoleSummary(role.getId(), role.getAppCode(), role.getRoleCode(), role.getRoleName(),
                    role.getStatus(), role.getRemark(), role.getCreateTime(), role.getUpdateTime());
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAppCode() {
            return appCode;
        }

        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public AppRole toEntity() {
            AppRole role = new AppRole();
            role.setAppCode(appCode);
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setStatus(status);
            role.setRemark(remark);
            return role;
        }
    }

    /**
     * 角色授权菜单请求体。
     */
    public static class GrantMenusRequest {
        /**
         * 菜单 ID 列表。
         */
        @NotNull(message = "menuIds 不能为空")
        private List<Long> menuIds;

        public List<Long> getMenuIds() {
            return menuIds;
        }

        public void setMenuIds(List<Long> menuIds) {
            this.menuIds = menuIds;
        }
    }

}
