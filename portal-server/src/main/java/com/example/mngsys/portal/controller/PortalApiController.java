package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.service.PortalAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal/api")
@Validated
public class PortalApiController {

    private final PortalAuthService portalAuthService;

    public PortalApiController(PortalAuthService portalAuthService) {
        this.portalAuthService = portalAuthService;
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

    @PostMapping("/sso/jump-url")
    public ApiResponse<SsoJumpResponse> ssoJumpUrl(@Valid @RequestBody SsoJumpRequest request) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED);
        }
        String jumpUrl = portalAuthService.createSsoJumpUrl(userId, request.getSystemCode(), request.getTargetUrl());
        return ApiResponse.success(new SsoJumpResponse(jumpUrl));
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
}
