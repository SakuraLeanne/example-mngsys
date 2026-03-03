package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * SSO 全局会话注销请求。
 */
public class PortalSsoLogoutRequest {
    @NotBlank(message = "systemCode不能为空")
    private String systemCode;
    @NotBlank(message = "gSessionId不能为空")
    private String gSessionId;
    @NotBlank(message = "logoutToken不能为空")
    private String logoutToken;

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getGSessionId() {
        return gSessionId;
    }

    public void setGSessionId(String gSessionId) {
        this.gSessionId = gSessionId;
    }

    public String getLogoutToken() {
        return logoutToken;
    }

    public void setLogoutToken(String logoutToken) {
        this.logoutToken = logoutToken;
    }
}
