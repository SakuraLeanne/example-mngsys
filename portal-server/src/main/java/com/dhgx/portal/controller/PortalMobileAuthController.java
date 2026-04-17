package com.dhgx.portal.controller;

import com.dhgx.common.portal.dto.PortalMiniProgramBindRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramLoginRequest;
import com.dhgx.common.portal.dto.PortalMobileLoginResponse;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.service.PortalMobileAuthService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * PortalMobileAuthController。
 * <p>
 * 移动端登录控制器，当前优先实现微信小程序登录和手机号绑定。
 * </p>
 */
@RestController
@Validated
public class PortalMobileAuthController {

    private final PortalMobileAuthService portalMobileAuthService;

    public PortalMobileAuthController(PortalMobileAuthService portalMobileAuthService) {
        this.portalMobileAuthService = portalMobileAuthService;
    }

    @PostMapping("/mobile/login/wechat/mini-program")
    public ApiResponse<PortalMobileLoginResponse> miniProgramLogin(@Valid @RequestBody PortalMiniProgramLoginRequest request) {
        return portalMobileAuthService.loginByMiniProgram(request);
    }

    @PostMapping("/mobile/bind/wechat-mobile")
    public ApiResponse<PortalMobileLoginResponse> bindMiniProgramMobile(@Valid @RequestBody PortalMiniProgramBindRequest request) {
        return portalMobileAuthService.bindMobileAndLogin(request);
    }

    @PostMapping("/mobile/token/refresh")
    public ApiResponse<PortalMobileLoginResponse> refresh(@RequestParam("refresh_token") String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "refresh_token 不能为空");
        }
        return portalMobileAuthService.refresh(refreshToken);
    }

    @PostMapping("/mobile/token/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String accessToken = resolveBearer(authorization);
        if (!StringUtils.hasText(accessToken)) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "access_token 不能为空");
        }
        return portalMobileAuthService.logout(accessToken);
    }

    @GetMapping("/mobile/token/introspect")
    public ApiResponse<PortalMobileAuthService.TokenPayload> introspect(@RequestParam("access_token") String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "access_token 不能为空");
        }
        return portalMobileAuthService.introspect(accessToken);
    }

    private String resolveBearer(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorization.startsWith(prefix)) {
            return authorization.substring(prefix.length()).trim();
        }
        return authorization.trim();
    }
}
