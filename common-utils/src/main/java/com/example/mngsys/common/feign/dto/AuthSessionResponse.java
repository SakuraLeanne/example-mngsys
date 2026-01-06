package com.example.mngsys.common.feign.dto;

/**
 * AuthSessionResponse。
 */
public class AuthSessionResponse {
    private String userId;

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
}
