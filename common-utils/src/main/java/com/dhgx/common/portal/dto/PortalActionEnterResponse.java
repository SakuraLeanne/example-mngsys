package com.dhgx.common.portal.dto;

/**
 * 动作票据进入响应。
 */
public class PortalActionEnterResponse {
    private final boolean success;
    private final String returnUrl;
    private final String systemCode;
    private final String ptk;

    public PortalActionEnterResponse(boolean success, String returnUrl, String systemCode, String ptk) {
        this.success = success;
        this.returnUrl = returnUrl;
        this.systemCode = systemCode;
        this.ptk = ptk;
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

    public String getPtk() {
        return ptk;
    }
}
