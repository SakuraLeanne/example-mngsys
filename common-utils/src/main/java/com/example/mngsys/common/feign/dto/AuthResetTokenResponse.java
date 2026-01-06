package com.example.mngsys.common.feign.dto;

/**
 * AuthResetTokenResponse。
 */
public class AuthResetTokenResponse {
    private String resetToken;
    private String mobile;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
