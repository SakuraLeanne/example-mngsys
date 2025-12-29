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
/**
 * AuthController。
 * <p>
 * 认证中心对外的 REST 控制器，包含登录、登出、查询当前会话、踢出会话等接口。
 * 依赖 Sa-Token 完成会话管理，并通过 {@link AuthService} 进行凭证校验。
 * </p>
 */
public class AuthController {

    /**
     * 认证业务服务，负责用户身份校验。
     */
    private final AuthService authService;

    /**
     * 认证相关配置，包含内部调用 Token 等安全参数。
     */
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    /**
     * 用户登录接口，校验用户名与密码成功后创建 Sa-Token 会话。
     *
     * @param request 登录请求体，包含用户名与密码
     * @return 携带用户基本信息的成功响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.User user = authService.authenticate(request.getUsername(), request.getPassword());
        StpUtil.login(user.getUserId());
        return ApiResponse.success(new LoginResponse(user.getUserId(), user.getUsername()));
    }

    /**
     * 用户登出接口，清理当前登录会话。
     *
     * @return 成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success(null);
    }

    /**
     * 查询当前登录用户会话信息，需要用户已登录。
     *
     * @return 包含用户 ID 的会话信息
     */
    @GetMapping("/session/me")
    public ApiResponse<SessionResponse> sessionMe() {
        StpUtil.checkLogin();
        String userId = String.valueOf(StpUtil.getLoginId());
        return ApiResponse.success(new SessionResponse(userId));
    }

    /**
     * 踢出指定用户的会话，仅允许内部服务凭借内部 Token 调用。
     *
     * @param internalToken 内部鉴权 Token，通过请求头传递
     * @param request       请求体，包含目标用户 ID
     * @return 失败时返回 401 未认证，成功时返回 OK
     */
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
        /** 用户名，必填。 */
        @NotBlank(message = "用户名不能为空")
        private String username;
        /** 密码，必填。 */
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
        /** 用户 ID。 */
        private final String userId;
        /** 用户名。 */
        private final String username;

        public LoginResponse(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class SessionResponse {
        /** 当前会话对应的用户 ID。 */
        private final String userId;

        public SessionResponse(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }
    }

    public static class KickRequest {
        /** 被踢出的用户 ID。 */
        @NotNull(message = "用户ID不能为空")
        private String userId;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
