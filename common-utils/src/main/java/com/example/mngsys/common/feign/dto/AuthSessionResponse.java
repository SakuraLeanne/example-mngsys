package com.example.mngsys.common.feign.dto;

/**
 * AuthSessionResponse。
 */
public class AuthSessionResponse {
    private String userId;
    private String username;
    private String mobile;

    public AuthSessionResponse() {
    }

    public AuthSessionResponse(String userId) {
        this.userId = userId;
    }

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
}
