package com.dhgx.common.portal.dto;

/**
 * 用户信息响应。
 */
public class PortalMeResponse {
    private final String userId;
    private final String username;
    private final String mobile;
    private final String email;
    private final Integer status;

    public PortalMeResponse(String userId, String username, String mobile, String email, Integer status) {
        this.userId = userId;
        this.username = username;
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
