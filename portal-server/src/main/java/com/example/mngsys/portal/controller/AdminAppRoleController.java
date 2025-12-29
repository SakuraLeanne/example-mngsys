package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminAppRoleService;
import org.springframework.validation.annotation.Validated;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端应用角色控制器，提供角色查询、创建、修改、授权菜单等接口。
 */
@RestController
@RequestMapping("/portal/api/admin/app-roles")
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
     * 创建角色。
     *
     * @param request            创建请求
     * @param httpServletRequest HTTP 请求，用于解析操作者 IP
     * @return 创建后的角色概要
     */
    @PostMapping
    public ApiResponse<RoleSummary> create(@Valid @RequestBody RoleCreateRequest request,
                                           HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppRoleService.Result<AppRole> result = portalAdminAppRoleService.createRole(
                request.toEntity(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(RoleSummary.from(result.getData()));
    }

    /**
     * 更新角色。
     *
     * @param id                 角色 ID
     * @param request            更新请求
     * @param httpServletRequest HTTP 请求，用于解析操作者 IP
     * @return 更新后的角色概要
     */
    @PutMapping("/{id}")
    public ApiResponse<RoleSummary> update(@PathVariable Long id,
                                           @Valid @RequestBody RoleUpdateRequest request,
                                           HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppRoleService.Result<AppRole> result = portalAdminAppRoleService.updateRole(
                id,
                request.toEntity(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(RoleSummary.from(result.getData()));
    }

    /**
     * 更新角色状态。
     *
     * @param id                 角色 ID
     * @param request            状态请求
     * @param httpServletRequest HTTP 请求，用于解析操作者 IP
     * @return 操作结果
     */
    @PostMapping("/{id}/status")
    public ApiResponse<ActionResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusRequest request,
                                                    HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppRoleService.Result<Void> result = portalAdminAppRoleService.updateStatus(
                id,
                request.getStatus(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 为角色授权菜单。
     *
     * @param id                 角色 ID
     * @param request            菜单授权请求
     * @param httpServletRequest HTTP 请求，用于解析操作者 IP
     * @return 操作结果
     */
    @PostMapping("/{id}/grant-menus")
    public ApiResponse<ActionResponse> grantMenus(@PathVariable Long id,
                                                  @Valid @RequestBody GrantMenusRequest request,
                                                  HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppRoleService.Result<Void> result = portalAdminAppRoleService.grantMenus(
                id,
                request.getMenuIds(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 从请求头和远端地址解析操作者 IP。
     *
     * @param request HTTP 请求
     * @return IP 地址
     */
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

    /**
     * 角色概要信息。
     */
    public static class RoleSummary {
        /**
         * 角色 ID。
         */
        private final Long id;
        /**
         * 应用编码。
         */
        private final String appCode;
        /**
         * 角色编码。
         */
        private final String roleCode;
        /**
         * 角色名称。
         */
        private final String roleName;
        /**
         * 状态。
         */
        private final Integer status;
        /**
         * 备注。
         */
        private final String remark;
        /**
         * 创建时间。
         */
        private final LocalDateTime createTime;
        /**
         * 更新时间。
         */
        private final LocalDateTime updateTime;

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

        public String getAppCode() {
            return appCode;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public Integer getStatus() {
            return status;
        }

        public String getRemark() {
            return remark;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }
    }

    /**
     * 角色创建请求体。
     */
    public static class RoleCreateRequest {
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
     * 角色更新请求体。
     */
    public static class RoleUpdateRequest {
        /**
         * 应用编码。
         */
        private String appCode;
        /**
         * 角色编码。
         */
        private String roleCode;
        /**
         * 角色名称。
         */
        private String roleName;
        /**
         * 状态。
         */
        private Integer status;
        /**
         * 备注。
         */
        private String remark;

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
     * 角色状态更新请求体。
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
