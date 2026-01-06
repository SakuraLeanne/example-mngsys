package com.example.mngsys.common.feign.dto;

/**
 * AuthPasswordResetRequest。
 */
public class AuthPasswordResetRequest {
    private String mobile;
    private String resetToken;
    private String encryptedPassword;
    private String newPassword;

    public AuthPasswordResetRequest() {
    }

    public AuthPasswordResetRequest(String mobile, String resetToken, String encryptedPassword, String newPassword) {
        this.mobile = mobile;
        this.resetToken = resetToken;
        this.encryptedPassword = encryptedPassword;
        this.newPassword = newPassword;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
