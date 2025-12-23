package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.service.PortalActionService;
import com.example.mngsys.portal.service.PortalAuthService;
import com.example.mngsys.portal.service.PortalPasswordService;
import com.example.mngsys.portal.service.PortalProfileService;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/portal/api")
@Validated
public class PortalApiController {

    private final PortalAuthService portalAuthService;
    private final PortalActionService portalActionService;
    private final PortalPasswordService portalPasswordService;
    private final PortalProfileService portalProfileService;

    public PortalApiController(PortalAuthService portalAuthService,
                               PortalActionService portalActionService,
                               PortalPasswordService portalPasswordService,
                               PortalProfileService portalProfileService) {
        this.portalAuthService = portalAuthService;
        this.portalActionService = portalActionService;
        this.portalPasswordService = portalPasswordService;
        this.portalProfileService = portalProfileService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        PortalAuthService.LoginResult loginResult = portalAuthService.login(
                request.getUsername(),
                request.getPassword(),
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

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        Long userId = RequestContext.getUserId();
        return ApiResponse.success(new MeResponse(userId));
    }

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

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(HttpServletRequest httpRequest) {
        String ptk = resolvePtk(httpRequest);
        PortalProfileService.ProfileResult result = portalProfileService.getProfile(ptk);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        ProfileResponse response = new ProfileResponse(result.getRealName(), result.getMobile(), result.getEmail());
        return ApiResponse.success(response);
    }

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

    @PostMapping("/sso/jump-url")
    public ApiResponse<SsoJumpResponse> ssoJumpUrl(@Valid @RequestBody SsoJumpRequest request) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED);
        }
        String jumpUrl = portalAuthService.createSsoJumpUrl(userId, request.getSystemCode(), request.getTargetUrl());
        return ApiResponse.success(new SsoJumpResponse(jumpUrl));
    }

    @PostMapping("/action/pwd/enter")
    public ApiResponse<ActionEnterResponse> enterPasswordAction(@Valid @RequestBody ActionTicketRequest request,
                                                                HttpServletResponse response) {
        return enterActionTicket(PortalActionService.ActionType.PASSWORD, request, response);
    }

    @PostMapping("/action/profile/enter")
    public ApiResponse<ActionEnterResponse> enterProfileAction(@Valid @RequestBody ActionTicketRequest request,
                                                               HttpServletResponse response) {
        return enterActionTicket(PortalActionService.ActionType.PROFILE, request, response);
    }

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

    private LoginResponse buildLoginResponse(Object data, String jumpUrl) {
        Long userId = null;
        String username = null;
        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            Object userIdValue = map.get("userId");
            if (userIdValue instanceof Number) {
                userId = ((Number) userIdValue).longValue();
            } else if (userIdValue != null) {
                userId = Long.parseLong(userIdValue.toString());
            }
            Object usernameValue = map.get("username");
            if (usernameValue != null) {
                username = usernameValue.toString();
            }
        }
        return new LoginResponse(userId, username, jumpUrl);
    }

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

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String systemCode;
        private String returnUrl;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
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

    public static class LoginResponse {
        private final Long userId;
        private final String username;
        private final String jumpUrl;

        public LoginResponse(Long userId, String username, String jumpUrl) {
            this.userId = userId;
            this.username = username;
            this.jumpUrl = jumpUrl;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getJumpUrl() {
            return jumpUrl;
        }
    }

    public static class PasswordChangeRequest {
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;
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

    public static class PasswordChangeResponse {
        private final boolean success;
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

    public static class ProfileResponse {
        private final String realName;
        private final String mobile;
        private final String email;

        public ProfileResponse(String realName, String mobile, String email) {
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
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
    }

    public static class ProfileUpdateRequest {
        private String realName;
        private String mobile;
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

    public static class ProfileUpdateResponse {
        private final boolean success;

        public ProfileUpdateResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    public static class MeResponse {
        private final Long userId;

        public MeResponse(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    public static class MenuItem {
        private final String code;
        private final String name;
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

    public static class SsoJumpRequest {
        @NotBlank(message = "systemCode 不能为空")
        private String systemCode;
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

    public static class SsoJumpResponse {
        private final String jumpUrl;

        public SsoJumpResponse(String jumpUrl) {
            this.jumpUrl = jumpUrl;
        }

        public String getJumpUrl() {
            return jumpUrl;
        }
    }

    public static class ActionTicketRequest {
        @NotBlank(message = "ticket 不能为空")
        private String ticket;

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }
    }

    public static class ActionEnterResponse {
        private final boolean ok;
        private final String returnUrl;
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
