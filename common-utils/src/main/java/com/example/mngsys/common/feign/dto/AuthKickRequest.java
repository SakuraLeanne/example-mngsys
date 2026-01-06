package com.example.mngsys.common.feign.dto;

/**
 * AuthKickRequest。
 */
public class AuthKickRequest {
    private String userId;

    public AuthKickRequest() {
    }

    public AuthKickRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
