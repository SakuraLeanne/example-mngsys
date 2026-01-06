package com.example.mngsys.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.mngsys.auth.common.api.ApiResponse;
import com.example.mngsys.auth.common.api.ErrorCode;
import com.example.mngsys.auth.config.AuthProperties;
import com.example.mngsys.auth.service.AuthService;
import com.example.mngsys.auth.service.PasswordCryptoService;
import com.example.mngsys.auth.service.PasswordResetService;
import com.example.mngsys.auth.service.SmsCodeService;
import com.example.mngsys.common.feign.dto.AuthKickRequest;
import com.example.mngsys.common.feign.dto.AuthLoginRequest;
import com.example.mngsys.common.feign.dto.AuthLoginResponse;
import com.example.mngsys.common.feign.dto.AuthLoginType;
import com.example.mngsys.common.feign.dto.AuthPasswordResetRequest;
import com.example.mngsys.common.feign.dto.AuthResetTokenResponse;
import com.example.mngsys.common.feign.dto.AuthSessionResponse;
import com.example.mngsys.common.feign.dto.AuthSmsScene;
import com.example.mngsys.common.feign.dto.AuthSmsSendRequest;
import com.example.mngsys.common.feign.dto.AuthSmsVerifyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@Validated
/**
 * AuthController。
 * <p>
 * 认证中心对外的 REST 控制器，包含登录、登出、查询当前会话、踢出会话等接口。
 * 依赖 Sa-Token 完成会话管理，并通过 {@link AuthService} 进行凭证校验。
 * </p>
 */
public class AuthController {

    private static final DateTimeFormatter LOGIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    @PostMapping("/login/sms/send")
    public ApiResponse<String> sendSms(@Valid @RequestBody AuthSmsSendRequest request) {
        String s = smsCodeService.sendCode(request.getMobile(), convertScene(request.getSceneOrDefault()));
        return ApiResponse.success(s);
    }

    /**
     * 校验短信验证码。
     *
     * @param request 请求体
     * @return 校验结果
     */
    @PostMapping("/login/sms/verify")
    public ApiResponse<Void> verifySms(@Valid @RequestBody AuthSmsVerifyRequest request) {
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
    public ApiResponse<String> sendForgotPasswordSms(@Valid @RequestBody AuthSmsSendRequest request) {
        String s = smsCodeService.sendCode(request.getMobile(), SmsCodeService.TemplateScene.VERIFICATION);
        return ApiResponse.success(s);
    }

    /**
     * 校验忘记密码验证码并下发重置令牌。
     *
     * @param request 请求体
     * @return 重置令牌
     */
    @PostMapping("/password/forgot/verify")
    public ApiResponse<AuthResetTokenResponse> verifyForgotPassword(@Valid @RequestBody AuthSmsVerifyRequest request) {
        smsCodeService.verifyCode(request.getMobile(), request.getCode());
        String token = passwordResetService.issueResetToken(request.getMobile());
        AuthResetTokenResponse response = new AuthResetTokenResponse();
        response.setResetToken(token);
        response.setMobile(request.getMobile());
        return ApiResponse.success(response);
    }

    /**
     * 根据手机号与重置令牌重置密码。
     *
     * @param request 重置请求
     * @return 重置结果
     */
    @PostMapping("/password/forgot/reset")
    public ApiResponse<Void> resetForgotPassword(@Valid @RequestBody AuthPasswordResetRequest request) {
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
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginType loginType = request.getLoginTypeOrDefault();
        AuthService.User user;
        switch (loginType) {
            case SMS:
                smsCodeService.verifyCode(request.getMobile(), request.getCode());
                user = authService.authenticateByMobile(request.getMobile());
                break;
            case USERNAME_PASSWORD:
                String decryptedPassword = resolvePasswordPayload(request);
                user = authService.authenticateByUsernameAndPassword(request.getUsername(), decryptedPassword);
                break;
            case QR_CODE:
                throw new IllegalArgumentException("暂未支持二维码登录");
            default:
                throw new IllegalArgumentException("不支持的登录方式");
        }
        StpUtil.login(user.getUserId());
        long loginTime = StpUtil.getSession().getCreateTime();
        AuthLoginResponse response = new AuthLoginResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setMobile(user.getMobile());
        response.setRealName(user.getRealName());
        response.setSatoken(StpUtil.getTokenValue());
        response.setLoginTime(formatLoginTime(loginTime));
        return ApiResponse.success(response);
    }

    private String resolvePasswordPayload(AuthLoginRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        AuthProperties.PasswordEncryptProperties encryptProps = authProperties.getPasswordEncrypt();
        boolean encryptEnabled = encryptProps != null && encryptProps.isEnabled();
        if (encryptEnabled) {
            if (!StringUtils.hasText(request.getEncryptedPassword())) {
                throw new IllegalArgumentException("密码密文不能为空");
            }
            return passwordCryptoService.decrypt(request.getEncryptedPassword(), null);
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return request.getPassword();
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
    @GetMapping("/session-info")
    public ApiResponse<AuthSessionResponse> sessionMe() {
        StpUtil.checkLogin();
        String userId = String.valueOf(StpUtil.getLoginId());
        return ApiResponse.success(new AuthSessionResponse(userId));
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
            @Valid @RequestBody AuthKickRequest request) {
        if (internalToken == null || !internalToken.equals(authProperties.getInternalToken())) {
            return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.getHttpStatus())
                    .body(ApiResponse.failure(ErrorCode.UNAUTHENTICATED, "内部鉴权失败"));
        }
        StpUtil.logout(request.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private SmsCodeService.TemplateScene convertScene(AuthSmsScene scene) {
        AuthSmsScene resolved = scene == null ? AuthSmsScene.LOGIN : scene;
        switch (resolved) {
            case VERIFICATION:
                return SmsCodeService.TemplateScene.VERIFICATION;
            case LOGIN:
            default:
                return SmsCodeService.TemplateScene.LOGIN;
        }
    }

    private String formatLoginTime(long loginTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(loginTime), ZoneId.systemDefault())
                .format(LOGIN_TIME_FORMATTER);
    }
}
