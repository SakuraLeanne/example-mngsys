package com.example.mngsys.common.portal.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码请求。
 */
public class PortalPasswordChangeRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码长度至少8位")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
