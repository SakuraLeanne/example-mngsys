package com.example.mngsys.common.portal.dto;

/**
 * 个人资料更新响应。
 */
public class PortalProfileUpdateResponse {
    private final boolean success;

    public PortalProfileUpdateResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
