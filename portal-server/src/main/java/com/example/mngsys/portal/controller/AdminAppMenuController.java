package com.example.mngsys.portal.controller;

import com.example.mngsys.common.api.ActionResponse;
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
    public ApiResponse<ActionResponse> create(@Valid @RequestBody AppMenuResource request) {
        if (request.getSort() == null) {
            request.setSort(0);
        }
        if (request.getStatus() == null) {
            request.setStatus(1);
        }
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.createMenu(
                request);
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
                                              @Valid @RequestBody AppMenuResource request) {
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.updateMenu(
                id,
                request);
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
     * @param status  状态值
     * @return 操作结果
     */
    @PostMapping("/{id}/status")
    public ApiResponse<ActionResponse> updateStatus(@PathVariable Long id,
                                                    @RequestParam
                                                    @NotNull(message = "status 不能为空") Integer status) {
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.updateStatus(
                id,
                status);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

}
