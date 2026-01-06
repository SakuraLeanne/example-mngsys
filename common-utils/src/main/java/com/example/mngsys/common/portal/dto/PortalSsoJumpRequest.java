package com.example.mngsys.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * SSO 跳转请求。
 */
public class PortalSsoJumpRequest {
    @NotBlank(message = "systemCode 不能为空")
    private String systemCode;
    @NotBlank(message = "targetUrl 不能为空")
    private String targetUrl;

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
