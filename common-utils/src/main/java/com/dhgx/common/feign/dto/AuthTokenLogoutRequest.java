package com.dhgx.common.feign.dto;

import javax.validation.constraints.NotBlank;

/**
 * 按 token 注销请求。
 */
public class AuthTokenLogoutRequest {
    @NotBlank(message = "tokenValue不能为空")
    private String tokenValue;

    public AuthTokenLogoutRequest() {
    }

    public AuthTokenLogoutRequest(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }
}
