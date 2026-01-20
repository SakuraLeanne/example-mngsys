package com.dhgx.common.portal.dto;

/**
 * 动作票据进入响应。
 */
public class PortalActionEnterResponse {
    private final boolean success;
    private final String returnUrl;
    private final String systemCode;

    public PortalActionEnterResponse(boolean success, String returnUrl, String systemCode) {
        this.success = success;
        this.returnUrl = returnUrl;
        this.systemCode = systemCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getSystemCode() {
        return systemCode;
    }
}
