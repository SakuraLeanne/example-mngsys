package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.service.PortalActionService;
import com.example.mngsys.portal.service.PortalAuthService;
import com.example.mngsys.portal.service.PortalPasswordService;
import com.example.mngsys.portal.service.PortalProfileService;
import com.example.mngsys.portal.service.PortalUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;

/**
 * Portal 接口控制器，负责门户登录、个人资料、动作票据等与用户交互的接口入口。
 */
@RestController
@RequestMapping("/portal/api")
@Validated
public class PortalApiController {

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
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        PortalAuthService.LoginResult loginResult = portalAuthService.login(
                request.getMobile(),
                request.getCode(),
                request.getSystemCode(),
                request.getReturnUrl());
        ResponseEntity<ApiResponse> authResponse = loginResult.getResponseEntity();
        if (authResponse != null) {
            List<String> setCookies = authResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                setCookies.forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie));
            }
        }
        ApiResponse authBody = loginResult.getResponseBody();
        if (authBody == null) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应");
        }
        if (authBody.getCode() != 0) {
            if (authBody.getCode() == ErrorCode.INVALID_ARGUMENT.getCode()) {
                return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, authBody.getMessage());
            }
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, authBody.getMessage());
        }
        LoginResponse loginResponse = buildLoginResponse(authBody.getData(), loginResult.getJumpUrl());
        return ApiResponse.success(loginResponse);
    }

    /**
     * 发送登录短信验证码。
     *
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSms(@Valid @RequestBody SmsSendRequest request) {
        ApiResponse<Void> resp = authClient.sendLoginSms(request.getMobile());
        return resp == null ? ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应") : resp;
    }

    /**
     * 忘记密码 - 发送验证码。
     *
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/password/forgot/send")
    public ApiResponse<Void> sendForgotPasswordSms(@Valid @RequestBody SmsSendRequest request) {
        ApiResponse<Void> resp = authClient.sendForgotPasswordSms(request.getMobile());
        return resp == null ? ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应") : resp;
    }

    /**
     * 忘记密码 - 校验验证码并获取重置令牌。
     *
     * @param request 请求体
     * @return 重置令牌
     */
    @PostMapping("/password/forgot/verify")
    public ApiResponse<?> verifyForgotPassword(@Valid @RequestBody SmsVerifyRequest request) {
        ApiResponse<AuthClient.ResetTokenResponse> resp = authClient.verifyForgotPassword(request.getMobile(), request.getCode());
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
    public ApiResponse<?> resetForgotPassword(@Valid @RequestBody ForgotPasswordResetRequest request) {
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
    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
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
        MeResponse response = new MeResponse(
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
    public ApiResponse<List<MenuItem>> menus() {
        List<MenuItem> menus = Arrays.asList(
                new MenuItem("dashboard", "仪表盘", "/portal/dashboard"),
                new MenuItem("users", "用户管理", "/portal/users"),
                new MenuItem("roles", "角色管理", "/portal/roles"),
                new MenuItem("menus", "菜单资源", "/portal/menus"),
                new MenuItem("audit", "审计日志", "/portal/audit"),
                new MenuItem("settings", "系统设置", "/portal/settings")
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
        ResponseEntity<ApiResponse> authResponse = portalAuthService.logout(cookie);
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
    public ApiResponse<PasswordChangeResponse> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                                              HttpServletRequest httpRequest) {
        String ptk = resolvePtk(httpRequest);
        PortalPasswordService.ChangeResult result = portalPasswordService.changePassword(
                request.getOldPassword(),
                request.getNewPassword(),
                ptk);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new PasswordChangeResponse(true, true));
    }

    /**
     * 查询个人资料。
     *
     * @param httpRequest HTTP 请求，用于获取 ptk
     * @return 个人资料信息
     */
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(HttpServletRequest httpRequest) {
        String ptk = resolvePtk(httpRequest);
        PortalProfileService.ProfileResult result = portalProfileService.getProfile(ptk);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        ProfileResponse response = new ProfileResponse(
                result.getUserId(),
                result.getUsername(),
                result.getRealName(),
                result.getMobile(),
                result.getEmail(),
                result.getStatus());
        return ApiResponse.success(response);
    }

    /**
     * 更新个人资料。
     *
     * @param request     更新请求
     * @param httpRequest HTTP 请求，用于读取 ptk
     * @return 更新结果
     */
    @PostMapping("/profile")
    public ApiResponse<ProfileUpdateResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                            HttpServletRequest httpRequest) {
        String ptk = resolvePtk(httpRequest);
        PortalProfileService.UpdateResult result = portalProfileService.updateProfile(
                ptk,
                request.getRealName(),
                request.getMobile(),
                request.getEmail());
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new ProfileUpdateResponse(true));
    }

    /**
     * 生成单点登录跳转链接。
     *
     * @param request 生成跳转请求参数
     * @return 跳转链接响应
     */
    @PostMapping("/sso/jump-url")
    public ApiResponse<SsoJumpResponse> ssoJumpUrl(@Valid @RequestBody SsoJumpRequest request) {
        String userId = RequestContext.getUserId();
        if (userId == null) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED);
        }
        String jumpUrl = portalAuthService.createSsoJumpUrl(userId, request.getSystemCode(), request.getTargetUrl());
        return ApiResponse.success(new SsoJumpResponse(jumpUrl));
    }

    /**
     * 进入密码校验动作。
     *
     * @param request  动作票据请求
     * @param response HTTP 响应，用于写入新的 ptk
     * @return 动作校验结果
     */
    @PostMapping("/action/pwd/enter")
    public ApiResponse<ActionEnterResponse> enterPasswordAction(@Valid @RequestBody ActionTicketRequest request,
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
    public ApiResponse<ActionEnterResponse> enterProfileAction(@Valid @RequestBody ActionTicketRequest request,
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
    private ApiResponse<ActionEnterResponse> enterActionTicket(PortalActionService.ActionType actionType,
                                                               ActionTicketRequest request,
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
        ActionEnterResponse payload = new ActionEnterResponse(true, result.getReturnUrl(), result.getSystemCode());
        return ApiResponse.success(payload);
    }

    /**
     * 构造登录响应，解析返回的用户信息与跳转地址。
     *
     * @param data    认证服务返回的数据体
     * @param jumpUrl 跳转地址
     * @return 登录响应对象
     */
    private LoginResponse buildLoginResponse(Object data, String jumpUrl) {
        String userId = null;
        String username = null;
        String mobile = null;
        String realName = null;
        String satoken = null;
        Long loginTime = null;
        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            Object userIdValue = map.get("userId");
            if (userIdValue instanceof Number) {
                userId = userIdValue.toString();
            } else if (userIdValue != null) {
                userId = userIdValue.toString();
            }
            Object usernameValue = map.get("username");
            if (usernameValue != null) {
                username = usernameValue.toString();
            }
            Object mobileValue = map.get("mobile");
            if (mobileValue != null) {
                mobile = mobileValue.toString();
            }
            Object realNameValue = map.get("realName");
            if (realNameValue != null) {
                realName = realNameValue.toString();
            }
            Object tokenValue = map.get("satoken");
            if (tokenValue != null) {
                satoken = tokenValue.toString();
            }
            Object loginTimeValue = map.get("loginTime");
            if (loginTimeValue instanceof Number) {
                loginTime = ((Number) loginTimeValue).longValue();
            } else if (loginTimeValue != null) {
                try {
                    loginTime = Long.parseLong(loginTimeValue.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new LoginResponse(userId, username, mobile, realName, satoken, loginTime, jumpUrl);
    }

    /**
     * 从请求中解析 ptk cookie。
     *
     * @param request HTTP 请求
     * @return ptk 值
     */
    private String resolvePtk(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        Optional<Cookie> cookie = Arrays.stream(cookies)
                .filter(item -> "ptk".equals(item.getName()))
                .findFirst();
        return cookie.map(Cookie::getValue).orElse(null);
    }

    /**
     * 登录请求体，包含手机号、验证码及跳转参数。
     */
    public static class LoginRequest {
        /**
         * 手机号。
         */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /**
         * 短信验证码。
         */
        @NotBlank(message = "验证码不能为空")
        private String code;
        /**
         * 系统编码。
         */
        private String systemCode;
        /**
         * 登录成功后的返回地址。
         */
        private String returnUrl;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSystemCode() {
            return systemCode;
        }

        public void setSystemCode(String systemCode) {
            this.systemCode = systemCode;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }
    }

    /**
     * 短信发送请求体。
     */
    public static class SmsSendRequest {
        /**
         * 手机号。
         */
        @NotBlank(message = "手机号不能为空")
        private String mobile;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
    }

    /**
     * 短信校验请求体。
     */
    public static class SmsVerifyRequest {
        /**
         * 手机号。
         */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /**
         * 验证码。
         */
        @NotBlank(message = "验证码不能为空")
        private String code;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    /**
     * 忘记密码重置请求体。
     */
    public static class ForgotPasswordResetRequest {
        /**
         * 手机号。
         */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /**
         * 重置令牌。
         */
        @NotBlank(message = "重置令牌不能为空")
        private String resetToken;
        /**
         * 密码密文（Base64 AES/GCM）。
         */
        private String encryptedPassword;
        /**
         * 明文密码（仅在未开启加密校验时生效）。
         */
        @Size(max = 128, message = "密码长度过长")
        private String newPassword;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getResetToken() {
            return resetToken;
        }

        public void setResetToken(String resetToken) {
            this.resetToken = resetToken;
        }

        public String getEncryptedPassword() {
            return encryptedPassword;
        }

        public void setEncryptedPassword(String encryptedPassword) {
            this.encryptedPassword = encryptedPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * 登录响应体，包含用户 ID、用户名与跳转链接。
     */
    public static class LoginResponse {
        /**
         * 用户主键 ID。
         */
        private final String userId;
        /**
         * 用户名。
         */
        private final String username;
        /**
         * 手机号。
         */
        private final String mobile;
        /**
         * 真实姓名。
         */
        private final String realName;
        /**
         * Sa-Token token 值。
         */
        private final String satoken;
        /**
         * 登录时间戳（毫秒）。
         */
        private final Long loginTime;
        /**
         * 跳转链接。
         */
        private final String jumpUrl;

        public LoginResponse(String userId, String username, String mobile, String realName, String satoken, Long loginTime, String jumpUrl) {
            this.userId = userId;
            this.username = username;
            this.mobile = mobile;
            this.realName = realName;
            this.satoken = satoken;
            this.loginTime = loginTime;
            this.jumpUrl = jumpUrl;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getMobile() {
            return mobile;
        }

        public String getRealName() {
            return realName;
        }

        public String getSatoken() {
            return satoken;
        }

        public Long getLoginTime() {
            return loginTime;
        }

        public String getJumpUrl() {
            return jumpUrl;
        }
    }

    /**
     * 修改密码请求体。
     */
    public static class PasswordChangeRequest {
        /**
         * 原密码。
         */
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;
        /**
         * 新密码。
         */
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, message = "新密码长度至少8位")
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * 修改密码响应体，标识是否成功以及是否需要重新登录。
     */
    public static class PasswordChangeResponse {
        /**
         * 是否修改成功。
         */
        private final boolean success;
        /**
         * 是否需要重新登录。
         */
        private final boolean needRelogin;

        public PasswordChangeResponse(boolean success, boolean needRelogin) {
            this.success = success;
            this.needRelogin = needRelogin;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isNeedRelogin() {
            return needRelogin;
        }
    }

    /**
     * 个人资料响应体。
     */
    public static class ProfileResponse {
        /**
         * 用户 ID。
         */
        private final String userId;
        /**
         * 用户名。
         */
        private final String username;
        /**
         * 真实姓名。
         */
        private final String realName;
        /**
         * 手机号。
         */
        private final String mobile;
        /**
         * 邮箱。
         */
        private final String email;
        /**
         * 用户状态。
         */
        private final Integer status;

        public ProfileResponse(String userId, String username, String realName, String mobile, String email, Integer status) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRealName() {
            return realName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getEmail() {
            return email;
        }

        public Integer getStatus() {
            return status;
        }
    }

    /**
     * 个人资料更新请求体。
     */
    public static class ProfileUpdateRequest {
        /**
         * 真实姓名。
         */
        private String realName;
        /**
         * 手机号。
         */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /**
         * 邮箱。
         */
        private String email;

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * 个人资料更新结果。
     */
    public static class ProfileUpdateResponse {
        /**
         * 是否更新成功。
         */
        private final boolean success;

        public ProfileUpdateResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    /**
     * 用户信息响应，仅包含用户 ID。
     */
    public static class MeResponse {
        /**
         * 用户 ID。
         */
        private final String userId;
        /**
         * 用户名。
         */
        private final String username;
        /**
         * 手机号。
         */
        private final String mobile;
        /**
         * 邮箱。
         */
        private final String email;
        /**
         * 用户状态。
         */
        private final Integer status;

        public MeResponse(String userId, String username, String mobile, String email, Integer status) {
            this.userId = userId;
            this.username = username;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getMobile() {
            return mobile;
        }

        public String getEmail() {
            return email;
        }

        public Integer getStatus() {
            return status;
        }
    }

    /**
     * 菜单项信息。
     */
    public static class MenuItem {
        /**
         * 菜单编码。
         */
        private final String code;
        /**
         * 菜单名称。
         */
        private final String name;
        /**
         * 菜单路径。
         */
        private final String path;

        public MenuItem(String code, String name, String path) {
            this.code = code;
            this.name = name;
            this.path = path;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }
    }

    /**
     * 生成 SSO 跳转链接的请求。
     */
    public static class SsoJumpRequest {
        /**
         * 系统编码。
         */
        @NotBlank(message = "systemCode 不能为空")
        private String systemCode;
        /**
         * 目标地址。
         */
        @NotBlank(message = "targetUrl 不能为空")
        private String targetUrl;

        public String getSystemCode() {
            return systemCode;
        }

        public void setSystemCode(String systemCode) {
            this.systemCode = systemCode;
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
        }
    }

    /**
     * SSO 跳转响应，包含完整的跳转 URL。
     */
    public static class SsoJumpResponse {
        /**
         * 跳转 URL。
         */
        private final String jumpUrl;

        public SsoJumpResponse(String jumpUrl) {
            this.jumpUrl = jumpUrl;
        }

        public String getJumpUrl() {
            return jumpUrl;
        }
    }

    /**
     * 动作票据请求，包含 ticket。
     */
    public static class ActionTicketRequest {
        /**
         * 动作 ticket。
         */
        @NotBlank(message = "ticket 不能为空")
        private String ticket;

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }
    }

    /**
     * 动作进入响应，描述动作处理结果与跳转信息。
     */
    public static class ActionEnterResponse {
        /**
         * 是否处理成功。
         */
        private final boolean ok;
        /**
         * 返回地址。
         */
        private final String returnUrl;
        /**
         * 系统编码。
         */
        private final String systemCode;

        public ActionEnterResponse(boolean ok, String returnUrl, String systemCode) {
            this.ok = ok;
            this.returnUrl = returnUrl;
            this.systemCode = systemCode;
        }

        public boolean isOk() {
            return ok;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public String getSystemCode() {
            return systemCode;
        }
    }
}
