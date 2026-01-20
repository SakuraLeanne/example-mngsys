package com.dhgx.portal.controller;

import com.dhgx.portal.common.PortalActionTicketUtil;
import com.dhgx.portal.common.api.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器，用于开发联调生成 action_ticket。
 */
@RestController
@RequestMapping("/test/action-ticket")
@Validated
public class TestController {

    private final PortalActionTicketUtil portalActionTicketUtil;

    public TestController(PortalActionTicketUtil portalActionTicketUtil) {
        this.portalActionTicketUtil = portalActionTicketUtil;
    }

    /**
     * 生成修改密码 action_ticket 并返回跳转链接。
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return 跳转链接
     */
    @GetMapping("/pwd")
    public ApiResponse<Map<String, String>> createPwdTicket(@RequestParam("userId") long userId,
                                                            @RequestParam("returnUrl") String returnUrl) {
        String jumpUrl = portalActionTicketUtil.createPwdChangeJumpUrl(userId, returnUrl);
        Map<String, String> data = new HashMap<>();
        data.put("jumpUrl", jumpUrl);
        return ApiResponse.success(data);
    }

    /**
     * 生成个人信息维护 action_ticket 并返回跳转链接。
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return 跳转链接
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, String>> createProfileTicket(@RequestParam("userId") long userId,
                                                                @RequestParam("returnUrl") String returnUrl) {
        String jumpUrl = portalActionTicketUtil.createProfileEditJumpUrl(userId, returnUrl);
        Map<String, String> data = new HashMap<>();
        data.put("jumpUrl", jumpUrl);
        return ApiResponse.success(data);
    }
}
