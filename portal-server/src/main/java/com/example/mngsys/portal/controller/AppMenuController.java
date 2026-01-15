package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.controller.dto.AppMenuTreeNode;
import com.example.mngsys.portal.service.AppMenuDeliveryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台应用菜单控制器，按用户返回可访问的菜单树。
 */
@RestController
@RequestMapping("/app/menus")
@Validated
public class AppMenuController {

    /**
     * 菜单下发服务。
     */
    private final AppMenuDeliveryService appMenuDeliveryService;

    /**
     * 构造函数，注入菜单下发服务。
     *
     * @param appMenuDeliveryService 菜单下发服务
     */
    public AppMenuController(AppMenuDeliveryService appMenuDeliveryService) {
        this.appMenuDeliveryService = appMenuDeliveryService;
    }

    /**
     * 查询当前用户可用菜单。
     *
     * @return 菜单树列表
     */
    @GetMapping
    public ApiResponse<List<AppMenuTreeNode>> menus() {
        String userId = RequestContext.getUserId();
        List<AppMenuTreeNode> menus = appMenuDeliveryService.loadMenus(userId);
        return ApiResponse.success(menus);
    }
}
