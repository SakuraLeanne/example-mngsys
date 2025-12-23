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

@RestController
@RequestMapping("/portal/api/app/menus")
@Validated
/**
 * AppMenuController。
 */
public class AppMenuController {

    private final AppMenuDeliveryService appMenuDeliveryService;

    public AppMenuController(AppMenuDeliveryService appMenuDeliveryService) {
        this.appMenuDeliveryService = appMenuDeliveryService;
    }

    @GetMapping
    public ApiResponse<List<AppMenuTreeNode>> menus() {
        Long userId = RequestContext.getUserId();
        List<AppMenuTreeNode> menus = appMenuDeliveryService.loadMenus(userId);
        return ApiResponse.success(menus);
    }
}
