package com.dhgx.portal.controller;

import com.dhgx.portal.common.PortalActionTicketUtil;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.exception.LocalizedBusinessException;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.service.PortalUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
    private final PortalUserService portalUserService;

    public TestController(PortalActionTicketUtil portalActionTicketUtil,
                          PortalUserService portalUserService) {
        this.portalActionTicketUtil = portalActionTicketUtil;
        this.portalUserService = portalUserService;
    }

    /**
     * 生成修改密码 action_ticket 并返回跳转链接。
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return 跳转链接
     */
    @GetMapping("/pwd")
    public ApiResponse<Map<String, String>> createPwdTicket(
            @RequestParam("userId") @NotNull(message = "不能为空") @Min(value = 1, message = "必须大于0") Long userId,
            @RequestParam("returnUrl") @NotBlank(message = "不能为空") String returnUrl) {
        validateUser(userId);
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
    public ApiResponse<Map<String, String>> createProfileTicket(
            @RequestParam("userId") @NotNull(message = "不能为空") @Min(value = 1, message = "必须大于0") Long userId,
            @RequestParam("returnUrl") @NotBlank(message = "不能为空") String returnUrl) {
        validateUser(userId);
        String jumpUrl = portalActionTicketUtil.createProfileEditJumpUrl(userId, returnUrl);
        Map<String, String> data = new HashMap<>();
        data.put("jumpUrl", jumpUrl);
        return ApiResponse.success(data);
    }

    private void validateUser(Long userId) {
        PortalUser user = portalUserService.getById(String.valueOf(userId));
        if (user == null) {
            throw new LocalizedBusinessException(ErrorCode.NOT_FOUND, null, "用户不存在");
        }
        Integer status = user.getStatus();
        if (status == null || status != 1) {
            throw new LocalizedBusinessException(ErrorCode.USER_DISABLED, null, "账号已被停用，请联系管理员");
        }
    }
}
