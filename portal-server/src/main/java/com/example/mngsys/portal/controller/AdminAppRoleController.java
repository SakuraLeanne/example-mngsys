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

@RestController
@RequestMapping("/portal/api/admin/app-roles")
@Validated
@AdminRequired
/**
 * AdminAppRoleController。
 */
public class AdminAppRoleController {

    private final PortalAdminAppRoleService portalAdminAppRoleService;

    public AdminAppRoleController(PortalAdminAppRoleService portalAdminAppRoleService) {
        this.portalAdminAppRoleService = portalAdminAppRoleService;
    }

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

    @PostMapping
    public ApiResponse<RoleSummary> create(@Valid @RequestBody RoleCreateRequest request,
                                           HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
        PortalAdminAppRoleService.Result<AppRole> result = portalAdminAppRoleService.createRole(
                request.toEntity(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(RoleSummary.from(result.getData()));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleSummary> update(@PathVariable Long id,
                                           @Valid @RequestBody RoleUpdateRequest request,
                                           HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
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

    @PostMapping("/{id}/status")
    public ApiResponse<ActionResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusRequest request,
                                                    HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
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

    @PostMapping("/{id}/grant-menus")
    public ApiResponse<ActionResponse> grantMenus(@PathVariable Long id,
                                                  @Valid @RequestBody GrantMenusRequest request,
                                                  HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
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

    public static class RoleCreateRequest {
        @NotBlank(message = "appCode 不能为空")
        private String appCode;
        @NotBlank(message = "roleCode 不能为空")
        private String roleCode;
        @NotBlank(message = "roleName 不能为空")
        private String roleName;
        private Integer status;
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

    public static class RoleUpdateRequest {
        private String appCode;
        private String roleCode;
        private String roleName;
        private Integer status;
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

    public static class GrantMenusRequest {
        @NotNull(message = "menuIds 不能为空")
        private List<Long> menuIds;

        public List<Long> getMenuIds() {
            return menuIds;
        }

        public void setMenuIds(List<Long> menuIds) {
            this.menuIds = menuIds;
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
