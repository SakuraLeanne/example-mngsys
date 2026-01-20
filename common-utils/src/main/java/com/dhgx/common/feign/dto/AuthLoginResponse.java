package com.dhgx.common.feign.dto;

/**
 * AuthLoginResponse。
 */
public class AuthLoginResponse {
    private String userId;
    private String username;
    private String mobile;
    private String realName;
    private String satoken;
    /**
     * 登录时间（格式：yyyy-MM-dd HH:mm:ss）。
     */
    private String loginTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getSatoken() {
        return satoken;
    }

    public void setSatoken(String satoken) {
        this.satoken = satoken;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }
}
