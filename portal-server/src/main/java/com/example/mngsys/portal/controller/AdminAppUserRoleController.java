package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminAppUserRoleService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端应用用户角色控制器，提供用户角色查询与授权接口。
 */
@RestController
@RequestMapping("/portal/api/admin/app-users/{userId}/roles")
@Validated
@AdminRequired
public class AdminAppUserRoleController {

    /**
     * 应用用户角色管理服务。
     */
    private final PortalAdminAppUserRoleService portalAdminAppUserRoleService;

    /**
     * 构造函数，注入用户角色管理服务。
     *
     * @param portalAdminAppUserRoleService 用户角色管理服务
     */
    public AdminAppUserRoleController(PortalAdminAppUserRoleService portalAdminAppUserRoleService) {
        this.portalAdminAppUserRoleService = portalAdminAppUserRoleService;
    }

    /**
     * 查询指定用户已拥有的角色列表。
     *
     * @param userId 用户 ID
     * @return 角色概要列表
     */
    @GetMapping
    public ApiResponse<List<RoleSummary>> list(@PathVariable String userId) {
        PortalAdminAppUserRoleService.Result<List<AppRole>> result =
                portalAdminAppUserRoleService.listUserRoles(userId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        List<RoleSummary> roles = result.getData().stream()
                .map(RoleSummary::from)
                .collect(Collectors.toList());
        return ApiResponse.success(roles);
    }

    /**
     * 批量授权用户角色。
     *
     * @param userId              用户 ID
     * @param request             授权请求，包含角色 ID 列表
     * @param httpServletRequest  HTTP 请求，用于获取操作者 IP
     * @return 授权结果
     */
    @PostMapping
    public ApiResponse<ActionResponse> grant(@PathVariable String userId,
                                             @Valid @RequestBody GrantRolesRequest request,
                                             HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppUserRoleService.Result<Void> result = portalAdminAppUserRoleService.grantRoles(
                userId,
                request.getRoleIds(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 从请求头与远端地址解析操作者 IP。
     *
     * @param request HTTP 请求
     * @return IP 地址
     */
    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 角色概要信息，用于列表展示。
     */
    public static class RoleSummary {
        /**
         * 角色主键 ID。
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
         * 状态：1 启用，0 禁用等。
         */
        private final Integer status;
        /**
         * 备注信息。
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
     * 授权请求体，包含角色 ID 列表。
     */
    public static class GrantRolesRequest {
        /**
         * 角色 ID 列表。
         */
        @NotNull(message = "roleIds 不能为空")
        private List<Long> roleIds;

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }

    /**
     * 通用操作响应。
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
