package com.example.mngsys.common.portal.dto;

/**
 * 门户登录响应。
 */
public class PortalLoginResponse {
    private final String userId;
    private final String username;
    private final String mobile;
    private final String realName;
    private final String satoken;
    /** 登录时间（格式：yyyy-MM-dd HH:mm:ss）。 */
    private final String loginTime;
    /** 跳转链接。 */
    private final String jumpUrl;

    public PortalLoginResponse(String userId,
                               String username,
                               String mobile,
                               String realName,
                               String satoken,
                               String loginTime,
                               String jumpUrl) {
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

    public String getLoginTime() {
        return loginTime;
    }

    public String getJumpUrl() {
        return jumpUrl;
    }
}
