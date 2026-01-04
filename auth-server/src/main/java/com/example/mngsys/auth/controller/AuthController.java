package com.example.mngsys.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.mngsys.auth.common.api.ApiResponse;
import com.example.mngsys.auth.common.api.ErrorCode;
import com.example.mngsys.auth.config.AuthProperties;
import com.example.mngsys.auth.service.AuthService;
import com.example.mngsys.auth.service.PasswordCryptoService;
import com.example.mngsys.auth.service.PasswordResetService;
import com.example.mngsys.auth.service.SmsCodeService;
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
import javax.validation.constraints.Size;

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
     * 短信验证码服务。
     */
    private final SmsCodeService smsCodeService;
    /**
     * 密码重置服务。
     */
    private final PasswordResetService passwordResetService;
    /**
     * 密码解密服务。
     */
    private final PasswordCryptoService passwordCryptoService;

    /**
     * 认证相关配置，包含内部调用 Token 等安全参数。
     */
    private final AuthProperties authProperties;

    public AuthController(AuthService authService,
                          SmsCodeService smsCodeService,
                          PasswordResetService passwordResetService,
                          PasswordCryptoService passwordCryptoService,
                          AuthProperties authProperties) {
        this.authService = authService;
        this.smsCodeService = smsCodeService;
        this.passwordResetService = passwordResetService;
        this.passwordCryptoService = passwordCryptoService;
        this.authProperties = authProperties;
    }

    /**
     * 发送登录短信验证码。
     *
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSms(@Valid @RequestBody SmsSendRequest request) {
        smsCodeService.sendCode(request.getMobile(), request.getSceneOrDefault());
        return ApiResponse.success(null);
    }

    /**
     * 校验短信验证码。
     *
     * @param request 请求体
     * @return 校验结果
     */
    @PostMapping("/sms/verify")
    public ApiResponse<Void> verifySms(@Valid @RequestBody SmsVerifyRequest request) {
        smsCodeService.verifyCode(request.getMobile(), request.getCode());
        return ApiResponse.success(null);
    }

    /**
     * 发送忘记密码短信验证码。
     *
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/password/forgot/send")
    public ApiResponse<Void> sendForgotPasswordSms(@Valid @RequestBody SmsSendRequest request) {
        smsCodeService.sendCode(request.getMobile(), SmsCodeService.TemplateScene.VERIFICATION);
        return ApiResponse.success(null);
    }

    /**
     * 校验忘记密码验证码并下发重置令牌。
     *
     * @param request 请求体
     * @return 重置令牌
     */
    @PostMapping("/password/forgot/verify")
    public ApiResponse<ResetTokenResponse> verifyForgotPassword(@Valid @RequestBody SmsVerifyRequest request) {
        smsCodeService.verifyCode(request.getMobile(), request.getCode());
        String token = passwordResetService.issueResetToken(request.getMobile());
        return ApiResponse.success(new ResetTokenResponse(token));
    }

    /**
     * 根据手机号与重置令牌重置密码。
     *
     * @param request 重置请求
     * @return 重置结果
     */
    @PostMapping("/password/forgot/reset")
    public ApiResponse<Void> resetForgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        String decryptedPassword = passwordCryptoService.decrypt(request.getEncryptedPassword(), request.getNewPassword());
        passwordResetService.resetPassword(request.getMobile(), request.getResetToken(), decryptedPassword);
        return ApiResponse.success(null);
    }

    /**
     * 用户登录接口，校验短信验证码成功后创建 Sa-Token 会话。
     *
     * @param request 登录请求体，包含用户名与密码
     * @return 携带用户基本信息的成功响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginType loginType = request.getLoginType();
        AuthService.User user;
        switch (loginType) {
            case SMS:
                smsCodeService.verifyCode(request.getMobile(), request.getCode());
                user = authService.authenticateByMobile(request.getMobile());
                break;
            case USERNAME_PASSWORD:
                validatePasswordPayload(request);
                user = authService.authenticateByUsernameAndPassword(request.getUsername(), request.getPassword());
                break;
            case QR_CODE:
                throw new IllegalArgumentException("暂未支持二维码登录");
            default:
                throw new IllegalArgumentException("不支持的登录方式");
        }
        StpUtil.login(user.getUserId());
        return ApiResponse.success(new LoginResponse(
                user.getUserId(),
                user.getUsername(),
                user.getMobile(),
                user.getRealName(),
                StpUtil.getTokenValue(),
                StpUtil.getSession().getCreateTime()));
    }

    private void validatePasswordPayload(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
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
        /** 登录方式，默认为短信验证码登录。 */
        private LoginType loginType = LoginType.SMS;
        /** 手机号，短信登录必填。 */
        private String mobile;
        /** 短信验证码，短信登录必填。 */
        private String code;
        /** 用户名，用户名密码登录必填。 */
        private String username;
        /** 密码，用户名密码登录必填。 */
        @Size(max = 128, message = "密码长度过长")
        private String password;

        public LoginType getLoginType() {
            return loginType == null ? LoginType.SMS : loginType;
        }

        public void setLoginType(LoginType loginType) {
            this.loginType = loginType;
        }

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
        /** 手机号。 */
        private final String mobile;
        /** 真实姓名。 */
        private final String realName;
        /** Sa-Token。 */
        private final String satoken;
        /** 登录时间戳。 */
        private final long loginTime;

        public LoginResponse(String userId, String username, String mobile, String realName, String satoken, long loginTime) {
            this.userId = userId;
            this.username = username;
            this.mobile = mobile;
            this.realName = realName;
            this.satoken = satoken;
            this.loginTime = loginTime;
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

        public long getLoginTime() {
            return loginTime;
        }
    }

    public enum LoginType {
        /** 手机号验证码登录。 */
        SMS,
        /** 用户名密码登录。 */
        USERNAME_PASSWORD,
        /** 二维码登录，预留扩展。 */
        QR_CODE
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

    public static class SmsSendRequest {
        /** 手机号。 */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /** 短信场景。 */
        private SmsCodeService.TemplateScene scene = SmsCodeService.TemplateScene.LOGIN;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public SmsCodeService.TemplateScene getScene() {
            return scene;
        }

        public void setScene(SmsCodeService.TemplateScene scene) {
            this.scene = scene;
        }

        public SmsCodeService.TemplateScene getSceneOrDefault() {
            return scene == null ? SmsCodeService.TemplateScene.LOGIN : scene;
        }
    }

    public static class SmsVerifyRequest {
        /** 手机号。 */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /** 短信验证码。 */
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

    public static class PasswordResetRequest {
        /** 手机号。 */
        @NotBlank(message = "手机号不能为空")
        private String mobile;
        /** 重置令牌。 */
        @NotBlank(message = "重置令牌不能为空")
        private String resetToken;
        /** 密码密文（Base64 AES/GCM）。 */
        private String encryptedPassword;
        /** 明文密码（仅在未开启加密校验时生效）。 */
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

    public static class ResetTokenResponse {
        private final String resetToken;

        public ResetTokenResponse(String resetToken) {
            this.resetToken = resetToken;
        }

        public String getResetToken() {
            return resetToken;
        }
    }
}
