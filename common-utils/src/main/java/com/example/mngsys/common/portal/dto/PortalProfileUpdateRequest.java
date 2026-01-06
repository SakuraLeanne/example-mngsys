package com.example.mngsys.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * 个人资料更新请求。
 */
public class PortalProfileUpdateRequest {
    private String realName;
    @NotBlank(message = "手机号不能为空")
    private String mobile;
    private String email;

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
