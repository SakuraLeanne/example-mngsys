package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminAppUserRoleService;
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

@RestController
@RequestMapping("/portal/api/admin/app-users/{userId}/roles")
@Validated
@AdminRequired
/**
 * AdminAppUserRoleController。
 */
public class AdminAppUserRoleController {

    private final PortalAdminAppUserRoleService portalAdminAppUserRoleService;

    public AdminAppUserRoleController(PortalAdminAppUserRoleService portalAdminAppUserRoleService) {
        this.portalAdminAppUserRoleService = portalAdminAppUserRoleService;
    }

    @GetMapping
    public ApiResponse<List<RoleSummary>> list(@PathVariable Long userId) {
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

    @PostMapping
    public ApiResponse<ActionResponse> grant(@PathVariable Long userId,
                                             @Valid @RequestBody GrantRolesRequest request,
                                             HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
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

    public static class RoleSummary {
        private final Long id;
        private final String appCode;
        private final String roleCode;
        private final String roleName;
        private final Integer status;
        private final String remark;
        private final LocalDateTime createTime;
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

    public static class GrantRolesRequest {
        @NotNull(message = "roleIds 不能为空")
        private List<Long> roleIds;

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
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
