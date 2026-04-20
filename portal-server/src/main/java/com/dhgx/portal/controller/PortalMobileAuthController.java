package com.dhgx.portal.controller;

import com.dhgx.common.portal.dto.PortalAppWechatLoginRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramBindRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramLoginRequest;
import com.dhgx.common.portal.dto.PortalMobileLoginResponse;
import com.dhgx.common.portal.dto.PortalMobileSmsLoginRequest;
import com.dhgx.common.portal.dto.PortalMobileSmsSendRequest;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.service.PortalMobileAuthService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * PortalMobileAuthController。
 * <p>
 * 移动端登录控制器（APP / 小程序）。
 * 说明：
 * 1) 登录前接口（短信发送、短信登录、微信登录、微信绑定）应由网关白名单放行；
 * 2) token 相关接口（refresh/logout/introspect）用于令牌生命周期管理；
 * 3) introspect 为网关/内部服务调用，不建议开放给外网客户端。
 * </p>
 */
@RestController
@Validated
public class PortalMobileAuthController {

    private final PortalMobileAuthService portalMobileAuthService;

    public PortalMobileAuthController(PortalMobileAuthService portalMobileAuthService) {
        this.portalMobileAuthService = portalMobileAuthService;
    }

    /**
     * 场景：APP/小程序手机号验证码登录前，先下发短信验证码。
     */
    @PostMapping("/mobile/login/sms/send")
    public ApiResponse<String> sendSmsCode(@Valid @RequestBody PortalMobileSmsSendRequest request) {
        return portalMobileAuthService.sendLoginSms(request);
    }

    /**
     * 场景：APP/小程序通过手机号+验证码直接登录门户，成功后签发 access_token/refresh_token。
     */
    @PostMapping("/mobile/login/sms")
    public ApiResponse<PortalMobileLoginResponse> smsLogin(@Valid @RequestBody PortalMobileSmsLoginRequest request) {
        return portalMobileAuthService.loginBySms(request);
    }

    /**
     * 场景：APP 微信授权登录。
     * APP 先拿微信 code，再调用本接口，由门户后端换取 openid/unionid 并判断是否已绑定。
     */
    @PostMapping("/mobile/login/wechat/app")
    public ApiResponse<PortalMobileLoginResponse> appWechatLogin(@Valid @RequestBody PortalAppWechatLoginRequest request) {
        return portalMobileAuthService.loginByWechatApp(request);
    }

    /**
     * 场景：微信小程序 code 登录。
     * 小程序端 wx.login() 获取 code 后调用本接口。
     */
    @PostMapping("/mobile/login/wechat/mini-program")
    public ApiResponse<PortalMobileLoginResponse> miniProgramLogin(@Valid @RequestBody PortalMiniProgramLoginRequest request) {
        return portalMobileAuthService.loginByMiniProgram(request);
    }

    /**
     * 场景：微信登录未绑定手机号时，客户端提交 bindToken + 手机号验证码完成绑定并登录。
     */
    @PostMapping("/mobile/bind/wechat-mobile")
    public ApiResponse<PortalMobileLoginResponse> bindMiniProgramMobile(@Valid @RequestBody PortalMiniProgramBindRequest request) {
        return portalMobileAuthService.bindMobileAndLogin(request);
    }

    /**
     * 场景：access_token 过期后，客户端使用 refresh_token 换新 token。
     */
    @PostMapping("/mobile/token/refresh")
    public ApiResponse<PortalMobileLoginResponse> refresh(@RequestParam("refresh_token") String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "refresh_token 不能为空");
        }
        return portalMobileAuthService.refresh(refreshToken);
    }

    /**
     * 场景：客户端主动退出登录，删除当前 access_token 对应的会话信息。
     */
    @PostMapping("/mobile/token/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String accessToken = resolveBearer(authorization);
        if (!StringUtils.hasText(accessToken)) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "access_token 不能为空");
        }
        return portalMobileAuthService.logout(accessToken);
    }

    /**
     * 场景：网关内部校验 Bearer token。
     * 说明：通过 Authorization 头传递 access_token，避免 query 参数泄露。
     */
    @PostMapping("/mobile/token/introspect")
    public ApiResponse<PortalMobileAuthService.TokenPayload> introspect(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "access_token", required = false) String accessTokenQuery) {
        String accessToken = resolveBearer(authorization);
        if (!StringUtils.hasText(accessToken) && StringUtils.hasText(accessTokenQuery)) {
            // 兼容历史调用方式，后续建议下线 query 参数方式。
            accessToken = accessTokenQuery.trim();
        }
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
