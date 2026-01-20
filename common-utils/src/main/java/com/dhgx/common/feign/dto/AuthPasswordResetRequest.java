package com.dhgx.common.feign.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * AuthPasswordResetRequest。
 */
public class AuthPasswordResetRequest {
    @NotBlank(message = "手机号不能为空")
    private String mobile;
    @NotBlank(message = "重置令牌不能为空")
    private String resetToken;
    private String encryptedPassword;
    @Size(max = 128, message = "密码长度过长")
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
