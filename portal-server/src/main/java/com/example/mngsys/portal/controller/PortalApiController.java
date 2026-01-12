package com.example.mngsys.portal.controller;

import com.example.mngsys.common.feign.dto.AuthLoginResponse;
import com.example.mngsys.common.portal.dto.PortalActionEnterResponse;
import com.example.mngsys.common.portal.dto.PortalActionTicketRequest;
import com.example.mngsys.common.portal.dto.PortalForgotPasswordResetRequest;
import com.example.mngsys.common.portal.dto.PortalLoginRequest;
import com.example.mngsys.common.portal.dto.PortalLoginResponse;
import com.example.mngsys.common.portal.dto.PortalMeResponse;
import com.example.mngsys.common.portal.dto.PortalMenuItem;
import com.example.mngsys.common.portal.dto.PortalPasswordChangeRequest;
import com.example.mngsys.common.portal.dto.PortalPasswordChangeResponse;
import com.example.mngsys.common.portal.dto.PortalProfileUpdateResponse;
import com.example.mngsys.common.portal.dto.PortalSmsSendRequest;
import com.example.mngsys.common.portal.dto.PortalSmsVerifyRequest;
import com.example.mngsys.common.portal.dto.PortalSsoJumpRequest;
import com.example.mngsys.common.portal.dto.PortalSsoJumpResponse;
import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.service.PortalActionService;
import com.example.mngsys.portal.service.PortalAuthService;
import com.example.mngsys.portal.service.PortalPasswordService;
import com.example.mngsys.portal.service.PortalProfileService;
import com.example.mngsys.portal.service.PortalUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;

/**
 * Portal 接口控制器，负责门户登录、个人资料、动作票据等与用户交互的接口入口。
 */
@RestController
//@RequestMapping("/portal/api")
@Validated
public class PortalApiController {
    private static final String PTK_COOKIE_NAME = "ptk";
    private static final String SATOKEN_COOKIE_NAME = "satoken";

    /**
     * 认证相关业务服务，用于登录、登出和 SSO 跳转处理。
     */
    private final PortalAuthService portalAuthService;
    /**
     * 动作票据业务服务，提供敏感操作的入口校验。
     */
    private final PortalActionService portalActionService;
    /**
     * 密码业务服务，负责密码修改等逻辑。
     */
    private final PortalPasswordService portalPasswordService;
    /**
     * 个人资料业务服务，处理资料查询与更新。
     */
    private final PortalProfileService portalProfileService;
    /**
     * 用户基础信息服务。
     */
    private final PortalUserService portalUserService;
    /**
     * 鉴权服务客户端，用于短信发送。
     */
    private final AuthClient authClient;

    /**
     * 构造函数，注入门户相关的业务服务。
     *
     * @param portalAuthService     认证服务
     * @param portalActionService   动作票据服务
     * @param portalPasswordService 密码服务
     * @param portalProfileService  个人资料服务
     */
    public PortalApiController(PortalAuthService portalAuthService,
                               PortalActionService portalActionService,
                               PortalPasswordService portalPasswordService,
                               PortalProfileService portalProfileService,
                               PortalUserService portalUserService,
                               AuthClient authClient) {
        this.portalAuthService = portalAuthService;
        this.portalActionService = portalActionService;
        this.portalPasswordService = portalPasswordService;
        this.portalProfileService = portalProfileService;
        this.portalUserService = portalUserService;
        this.authClient = authClient;
    }

    /**
     * 登录接口，校验用户凭证并返回跳转链接与用户信息。
     *
     * @param request  登录请求体
     * @param response HTTP 响应对象，用于写入 cookie
     * @return 登录结果响应
     */
    @PostMapping("/login")
    public ApiResponse<PortalLoginResponse> login(@Valid @RequestBody PortalLoginRequest request, HttpServletResponse response) {
        PortalAuthService.LoginResult loginResult = portalAuthService.login(request);
        ResponseEntity<? extends ApiResponse<?>> authResponse = loginResult.getResponseEntity();
        if (authResponse != null) {
            List<String> setCookies = authResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                setCookies.forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie));
            }
        }
        ApiResponse<?> authBody = loginResult.getResponseBody();
        if (authBody == null) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应");
        }
        if (authBody.getCode() != 0) {
            if (authBody.getCode() == ErrorCode.INVALID_ARGUMENT.getCode()) {
                return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, authBody.getMessage());
            }
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, authBody.getMessage());
        }
        AuthLoginResponse data = (AuthLoginResponse) authBody.getData();
        PortalLoginResponse loginResponse = buildLoginResponse(data, loginResult.getJumpUrl());
        return ApiResponse.success(loginResponse);
    }

    /**
     * 发送登录短信验证码。
     *
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/login/sms/send")
    public ApiResponse<String> sendSms(@Valid @RequestBody PortalSmsSendRequest request) {
        ApiResponse<String> resp = authClient.sendLoginSms(request.getMobile());
        return resp == null ? ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应") : resp;
    }

    /**
     * 忘记密码 - 发送验证码。
     *
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/password/forgot/send")
    public ApiResponse<String> sendForgotPasswordSms(@Valid @RequestBody PortalSmsSendRequest request) {
        ApiResponse<String> resp = authClient.sendForgotPasswordSms(request.getMobile());
        return resp == null ? ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应") : resp;
    }

    /**
     * 忘记密码 - 校验验证码并获取重置令牌。
     *
     * @param request 请求体
     * @return 重置令牌
     */
    @PostMapping("/password/forgot/verify")
    public ApiResponse<?> verifyForgotPassword(@Valid @RequestBody PortalSmsVerifyRequest request) {
        ApiResponse<?> resp = authClient.verifyForgotPassword(request.getMobile(), request.getCode());
        if (resp == null) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应");
        }
        return resp;
    }

    /**
     * 忘记密码 - 重置密码（需加密传输）。
     *
     * @param request 重置请求
     * @return 重置结果
     */
    @PostMapping("/password/forgot/reset")
    public ApiResponse<?> resetForgotPassword(@Valid @RequestBody PortalForgotPasswordResetRequest request) {
        ApiResponse<Void> resp = authClient.resetForgotPassword(
                request.getMobile(),
                request.getResetToken(),
                request.getEncryptedPassword(),
                request.getNewPassword());
        return resp == null ? ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应") : resp;
    }

    /**
     * 查询当前登录用户基本信息。
     *
     * @return 当前用户基础信息
     */
    @GetMapping("/loginuser/session-info")
    public ApiResponse<PortalMeResponse> me() {
        String userId = RequestContext.getUserId();
        if (userId == null) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED);
        }
        com.example.mngsys.portal.entity.PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ApiResponse.failure(ErrorCode.NOT_FOUND);
        }
        Integer status = user.getStatus();
        if (!Objects.equals(status, 1)) {
            return ApiResponse.failure(ErrorCode.USER_DISABLED);
        }
        PortalMeResponse response = new PortalMeResponse(
                user.getId(),
                user.getUsername(),
                user.getMobile(),
                user.getEmail(),
                status);
        return ApiResponse.success(response);
    }

    /**
     * 获取门户首页菜单数据（示例数据）。
     *
     * @return 菜单列表
     */
    @GetMapping("/home/menus")
    public ApiResponse<List<PortalMenuItem>> menus() {
        List<PortalMenuItem> menus = Arrays.asList(
                new PortalMenuItem("dashboard", "仪表盘", "/portal/dashboard"),
                new PortalMenuItem("users", "用户管理", "/portal/users"),
                new PortalMenuItem("roles", "角色管理", "/portal/roles"),
                new PortalMenuItem("menus", "菜单资源", "/portal/menus"),
                new PortalMenuItem("audit", "审计日志", "/portal/audit"),
                new PortalMenuItem("settings", "系统设置", "/portal/settings")
        );
        return ApiResponse.success(menus);
    }

    /**
     * 退出登录，调用认证服务清理会话并删除 cookie。
     *
     * @param request  HTTP 请求，读取 cookie
     * @param response HTTP 响应，写入清理后的 cookie
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String cookie = request.getHeader(HttpHeaders.COOKIE);
        ResponseEntity<ApiResponse<Void>> authResponse = portalAuthService.logout(cookie);
        if (authResponse != null) {
            List<String> setCookies = authResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                setCookies.forEach(c -> response.addHeader(HttpHeaders.SET_COOKIE, c));
            }
        }
        return ApiResponse.success(null);
    }

    /**
     * 修改登录密码。
     *
     * @param request     修改密码请求体
     * @param httpRequest HTTP 请求，用于获取动作票据
     * @return 修改结果
     */
    @PostMapping("/password/change")
    public ApiResponse<PortalPasswordChangeResponse> changePassword(@Valid @RequestBody PortalPasswordChangeRequest request,
                                                                    HttpServletRequest httpRequest,
                                                                    HttpServletResponse httpResponse) {
        String ptk = resolvePtk(httpRequest);
        String satoken = resolveSaToken(httpRequest);
        PortalPasswordService.ChangeResult result = portalPasswordService.changePassword(
                request.getEncryptedOldPassword(),
                request.getOldPassword(),
                request.getEncryptedNewPassword(),
                request.getNewPassword(),
                ptk);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        clearActionCookieForPassword(httpResponse, ptk, satoken);
        return ApiResponse.success(new PortalPasswordChangeResponse(true, true));
    }

    /**
     * 查询个人资料。
     *
     * @param httpRequest HTTP 请求，用于获取 ptk
     * @return 个人资料信息
     */
    @GetMapping("/profile")
    public ApiResponse<PortalUser> profile(HttpServletRequest httpRequest) {
        String ptk = resolvePtk(httpRequest);
        PortalProfileService.ProfileResult result = portalProfileService.getProfile(ptk);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(result.getUser());
    }

    /**
     * 更新个人资料。
     *
     * @param request     更新请求
     * @param httpRequest HTTP 请求，用于读取 ptk
     * @return 更新结果
     */
    @PostMapping("/profile")
    public ApiResponse<PortalProfileUpdateResponse> updateProfile(@Valid @RequestBody PortalUser request,
                                                                  HttpServletRequest httpRequest,
                                                                  HttpServletResponse httpResponse) {
        String ptk = resolvePtk(httpRequest);
        PortalProfileService.UpdateResult result = portalProfileService.updateProfile(
                ptk,
                request);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        clearActionCookie(httpResponse, ptk);
        return ApiResponse.success(new PortalProfileUpdateResponse(true));
    }

    /**
     * 生成单点登录跳转链接。
     *
     * @param request 生成跳转请求参数
     * @return 跳转链接响应
     */
    @PostMapping("/sso/jump-url")
    public ApiResponse<PortalSsoJumpResponse> ssoJumpUrl(@Valid @RequestBody PortalSsoJumpRequest request) {
        String userId = RequestContext.getUserId();
        if (userId == null) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED);
        }
        String jumpUrl = portalAuthService.createSsoJumpUrl(userId, request.getSystemCode(), request.getTargetUrl());
        return ApiResponse.success(new PortalSsoJumpResponse(jumpUrl));
    }

    /**
     * 进入密码校验动作。
     *
     * @param request  动作票据请求
     * @param response HTTP 响应，用于写入新的 ptk
     * @return 动作校验结果
     */
    @PostMapping("/action/pwd/enter")
    public ApiResponse<PortalActionEnterResponse> enterPasswordAction(@Valid @RequestBody PortalActionTicketRequest request,
                                                                      HttpServletResponse response) {
        return enterActionTicket(PortalActionService.ActionType.PASSWORD, request, response);
    }

    /**
     * 进入个人资料校验动作。
     *
     * @param request  动作票据请求
     * @param response HTTP 响应，用于写入新的 ptk
     * @return 动作校验结果
     */
    @PostMapping("/action/profile/enter")
    public ApiResponse<PortalActionEnterResponse> enterProfileAction(@Valid @RequestBody PortalActionTicketRequest request,
                                                                     HttpServletResponse response) {
        return enterActionTicket(PortalActionService.ActionType.PROFILE, request, response);
    }

    /**
     * 通用动作票据处理，校验 ticket 并设置新的 ptk。
     *
     * @param actionType 动作类型
     * @param request    动作票据请求
     * @param response   HTTP 响应，用于写 cookie
     * @return 动作进入结果
     */
    private ApiResponse<PortalActionEnterResponse> enterActionTicket(PortalActionService.ActionType actionType,
                                                                     PortalActionTicketRequest request,
                                                                     HttpServletResponse response) {
        PortalActionService.EnterResult result = portalActionService.enter(actionType, request.getTicket());
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        ResponseCookie cookie = ResponseCookie.from("ptk", result.getPtk())
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        PortalActionEnterResponse payload = new PortalActionEnterResponse(true, result.getReturnUrl(), result.getSystemCode());
        return ApiResponse.success(payload);
    }

    /**
     * 构造登录响应，解析返回的用户信息与跳转地址。
     *
     * @param data    认证服务返回的数据体
     * @param jumpUrl 跳转地址
     * @return 登录响应对象
     */
    private PortalLoginResponse buildLoginResponse(AuthLoginResponse data, String jumpUrl) {
        String userId = data.getUserId();
        String username = data.getUsername();
        String mobile = data.getMobile();
        String realName = data.getRealName();
        String satoken = data.getSatoken();
        String loginTime = data.getLoginTime();

        return new PortalLoginResponse(userId, username, mobile, realName, satoken, loginTime, jumpUrl);
    }

    /**
     * 从请求中解析 ptk cookie。
     *
     * @param request HTTP 请求
     * @return ptk 值
     */
    private String resolvePtk(HttpServletRequest request) {
        return resolveCookie(request, PTK_COOKIE_NAME);
    }

    private String resolveSaToken(HttpServletRequest request) {
        return resolveCookie(request, SATOKEN_COOKIE_NAME);
    }

    private String resolveCookie(HttpServletRequest request, String name) {
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        Optional<Cookie> cookie = Arrays.stream(cookies)
                .filter(item -> name.equals(item.getName()))
                .findFirst();
        return cookie.map(Cookie::getValue).orElse(null);
    }

    private void clearPtkCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from("ptk", "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearSaTokenCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(SATOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearActionCookie(HttpServletResponse response, String ptk) {
        if (StringUtils.hasText(ptk)) {
            clearPtkCookie(response);
        }
    }

    private void clearActionCookieForPassword(HttpServletResponse response, String ptk, String satoken) {
        if (StringUtils.hasText(ptk)) {
            clearPtkCookie(response);
        }
        if (StringUtils.hasText(satoken)) {
            clearSaTokenCookie(response);
        }
    }
}
