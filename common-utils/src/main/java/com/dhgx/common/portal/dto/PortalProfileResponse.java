package com.dhgx.common.portal.dto;

/**
 * 个人资料响应。
 */
public class PortalProfileResponse {
    private final String userId;
    private final String username;
    private final String realName;
    private final String mobile;
    private final String email;
    private final Integer status;

    public PortalProfileResponse(String userId, String username, String realName, String mobile, String email, Integer status) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.mobile = mobile;
        this.email = email;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
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

    public Integer getStatus() {
        return status;
    }
}
