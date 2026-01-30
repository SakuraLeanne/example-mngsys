package com.dhgx.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhgx.common.api.PageResponse;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.context.RequestContext;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.security.AdminRequired;
import com.dhgx.portal.service.PortalAdminAppUserRoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端应用角色用户控制器，提供角色授权用户查询接口。
 */
@RestController
@RequestMapping("/admin/app-roles/{roleId}/users")
@Validated
public class AdminAppRoleUserController {

    /**
     * 应用用户角色管理服务。
     */
    private final PortalAdminAppUserRoleService portalAdminAppUserRoleService;

    /**
     * 构造函数，注入用户角色管理服务。
     *
     * @param portalAdminAppUserRoleService 用户角色管理服务
     */
    public AdminAppRoleUserController(PortalAdminAppUserRoleService portalAdminAppUserRoleService) {
        this.portalAdminAppUserRoleService = portalAdminAppUserRoleService;
    }

    /**
     * 根据角色 ID 分页查询已授权用户列表。
     *
     * @param roleId   角色 ID
     * @param page     页码
     * @param size     页大小
     * @param username 用户名/真实姓名
     * @param mobile   手机号
     * @return 已授权用户分页数据
     */
    @GetMapping("/granted")
    @AdminRequired(scope = "app", allowAnyAppAdmin = true)
    public ApiResponse<PageResponse<PortalUser>> listGrantedUsers(@PathVariable Long roleId,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(required = false) String username,
                                                                  @RequestParam(required = false) String mobile) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppUserRoleService.Result<Page<PortalUser>> result =
                portalAdminAppUserRoleService.listRoleGrantedUsers(roleId, page, size, username, mobile, operatorId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        Page<PortalUser> pageResult = result.getData();
        PageResponse<PortalUser> response = new PageResponse<>(
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getRecords().size(),
                pageResult.getRecords()
        );
        return ApiResponse.success(response);
    }

    /**
     * 根据角色 ID 分页查询未授权用户列表。
     *
     * @param roleId   角色 ID
     * @param page     页码
     * @param size     页大小
     * @param username 用户名/真实姓名
     * @param mobile   手机号
     * @return 未授权用户分页数据
     */
    @GetMapping("/ungranted")
    @AdminRequired(scope = "app", allowAnyAppAdmin = true)
    public ApiResponse<PageResponse<PortalUser>> listUngrantedUsers(@PathVariable Long roleId,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(required = false) String username,
                                                                    @RequestParam(required = false) String mobile) {
        String operatorId = RequestContext.getUserId();
        PortalAdminAppUserRoleService.Result<Page<PortalUser>> result =
                portalAdminAppUserRoleService.listRoleUngrantedUsers(roleId, page, size, username, mobile, operatorId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        Page<PortalUser> pageResult = result.getData();
        PageResponse<PortalUser> response = new PageResponse<>(
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getRecords().size(),
                pageResult.getRecords()
        );
        return ApiResponse.success(response);
    }
}
