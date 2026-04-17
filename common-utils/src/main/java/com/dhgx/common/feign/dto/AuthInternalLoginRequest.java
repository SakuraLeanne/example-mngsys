package com.dhgx.common.feign.dto;

import javax.validation.constraints.NotBlank;

/**
 * AuthInternalLoginRequest。
 * <p>
 * 认证中心内部登录请求，仅用于服务间按 userId 建立会话。
 * </p>
 */
public class AuthInternalLoginRequest {

    @NotBlank(message = "userId 不能为空")
    private String userId;

    public AuthInternalLoginRequest() {
    }

    public AuthInternalLoginRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
