package com.example.mngsys.portal.controller;

import com.example.mngsys.common.api.ActionResponse;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.controller.dto.AppMenuTreeNode;
import com.example.mngsys.portal.entity.AppMenuResource;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminAppMenuService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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
     * 创建或更新菜单。
     *
     * @param request 菜单请求
     * @return 操作结果
     */
    @PostMapping
    public ApiResponse<ActionResponse> save(@Valid @RequestBody AppMenuResource request) {
        PortalAdminAppMenuService.Result<Void> result;
        if (request.getId() == null) {
            if (request.getSort() == null) {
                request.setSort(0);
            }
            if (request.getStatus() == null) {
                request.setStatus(1);
            }
            result = portalAdminAppMenuService.createMenu(request);
        } else {
            result = portalAdminAppMenuService.updateMenu(request.getId(), request);
        }
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode(), result.getMessage());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 批量删除菜单。
     *
     * @param request 删除请求
     * @return 操作结果
     */
    @PostMapping("/delete")
    public ApiResponse<ActionResponse> delete(@Valid @RequestBody DeleteMenuRequest request) {
        PortalAdminAppMenuService.Result<Void> result = portalAdminAppMenuService.deleteMenus(
                request.getIds());
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
    @GetMapping("/status")
    public ApiResponse<ActionResponse> updateStatus(@RequestParam@NotNull(message = "id 不能为空") Long id,
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

    public static class DeleteMenuRequest {
        @NotEmpty(message = "菜单ID不能为空")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

}
