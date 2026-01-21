package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * SSO ticket 校验请求。
 */
public class PortalSsoTicketVerifyRequest {
    @NotBlank(message = "systemCode不能为空")
    private String systemCode;
    @NotBlank(message = "ticket不能为空")
    private String ticket;
    @NotBlank(message = "redirectUri不能为空")
    private String redirectUri;
    private String state;

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
