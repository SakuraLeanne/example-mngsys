package com.example.mngsys.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.mngsys.auth.common.api.ApiResponse;
import com.example.mngsys.auth.common.api.ErrorCode;
import com.example.mngsys.auth.config.AuthProperties;
import com.example.mngsys.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/auth/api")
@Validated
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.User user = authService.authenticate(request.getUsername(), request.getPassword());
        StpUtil.login(user.getUserId());
        return ApiResponse.success(new LoginResponse(user.getUserId(), user.getUsername()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success(null);
    }

    @GetMapping("/session/me")
    public ApiResponse<SessionResponse> sessionMe() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(new SessionResponse(userId));
    }

    @PostMapping("/session/kick")
    public ResponseEntity<ApiResponse<Void>> kickSession(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody KickRequest request) {
        if (internalToken == null || !internalToken.equals(authProperties.getInternalToken())) {
            return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.getHttpStatus())
                    .body(ApiResponse.failure(ErrorCode.UNAUTHENTICATED, "内部鉴权失败"));
        }
        StpUtil.logout(request.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;

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
    }

    public static class LoginResponse {
        private final Long userId;
        private final String username;

        public LoginResponse(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class SessionResponse {
        private final Long userId;

        public SessionResponse(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    public static class KickRequest {
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
