package com.example.mngsys.common.feign.dto;

/**
 * AuthKickRequest。
 */
public class AuthKickRequest {
    @javax.validation.constraints.NotNull(message = "用户ID不能为空")
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
