package com.dhgx.common.portal.dto;

/**
 * SSO 跳转响应。
 */
public class PortalSsoJumpResponse {
    private final String jumpUrl;

    public PortalSsoJumpResponse(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public String getJumpUrl() {
        return jumpUrl;
    }
}
