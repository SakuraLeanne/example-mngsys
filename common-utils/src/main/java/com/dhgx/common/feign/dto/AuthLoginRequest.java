package com.dhgx.common.feign.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * AuthLoginRequest。
 * <p>
 * 统一的登录请求 DTO，支持短信验证码、用户名密码等方式。
 * </p>
 */
public class AuthLoginRequest {
    /** 登录方式，默认为短信验证码登录。 */
    @NotNull(message = "登录方式不能为空")
    private AuthLoginType loginType = AuthLoginType.SMS;
    /** 手机号，短信登录必填。 */
    private String mobile;
    /** 短信验证码，短信登录必填。 */
    private String code;
    /** 用户名，用户名密码登录必填。 */
    private String username;
    /** 密码，用户名密码登录必填。 */
    @Size(max = 128, message = "密码长度过长")
    private String password;
    /** 密码密文（Base64 AES/GCM），当启用密码传输加密时必填。 */
    private String encryptedPassword;

    public AuthLoginRequest() {
    }

    public AuthLoginRequest(String mobile, String code) {
        this.mobile = mobile;
        this.code = code;
        this.loginType = AuthLoginType.SMS;
    }

    public AuthLoginType getLoginType() {
        return loginType;
    }

    public AuthLoginType getLoginTypeOrDefault() {
        return loginType == null ? AuthLoginType.SMS : loginType;
    }

    public void setLoginType(AuthLoginType loginType) {
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

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }
}
