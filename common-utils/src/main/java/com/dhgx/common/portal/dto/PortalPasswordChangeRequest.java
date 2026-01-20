package com.dhgx.common.portal.dto;

import javax.validation.constraints.Size;

/**
 * 修改密码请求。
 */
public class PortalPasswordChangeRequest {
    private String encryptedOldPassword;
    private String oldPassword;
    private String encryptedNewPassword;
    @Size(max = 128, message = "密码长度过长")
    private String newPassword;

    public String getEncryptedOldPassword() {
        return encryptedOldPassword;
    }

    public void setEncryptedOldPassword(String encryptedOldPassword) {
        this.encryptedOldPassword = encryptedOldPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getEncryptedNewPassword() {
        return encryptedNewPassword;
    }

    public void setEncryptedNewPassword(String encryptedNewPassword) {
        this.encryptedNewPassword = encryptedNewPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
