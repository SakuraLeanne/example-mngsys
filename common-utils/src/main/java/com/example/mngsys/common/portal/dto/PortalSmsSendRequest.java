package com.example.mngsys.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * 门户短信发送请求。
 */
public class PortalSmsSendRequest {
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
