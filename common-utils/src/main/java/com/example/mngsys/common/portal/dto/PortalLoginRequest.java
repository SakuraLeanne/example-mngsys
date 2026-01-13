package com.example.mngsys.common.portal.dto;

import com.example.mngsys.common.feign.dto.AuthLoginType;

import javax.validation.constraints.Size;

/**
 * 门户登录请求。
 */
public class PortalLoginRequest {
    /** 登录方式，默认为短信验证码登录。 */
    private AuthLoginType loginType = AuthLoginType.SMS;
    /** 手机号，短信登录必填。 */
    private String mobile;
    /** 短信验证码，短信登录必填。 */
    private String code;
    /** 用户名，用户名密码登录必填。 */
    private String username;
    /** 明文密码，用户名密码登录时可选。 */
    @Size(max = 128, message = "密码长度过长")
    private String password;
    /** 密码密文，启用前端加密时使用。 */
    private String encryptedPassword;
    /** 图形验证码标识，用户名密码登录可能需要。 */
    private String captchaId;
    /** 图形验证码内容，用户名密码登录可能需要。 */
    private String captchaCode;
    /** 系统编码。 */
    private String systemCode;
    /** 登录成功后的返回地址。 */
    private String returnUrl;

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

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
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
