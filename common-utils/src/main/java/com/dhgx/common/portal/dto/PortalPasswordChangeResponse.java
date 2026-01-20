package com.dhgx.common.portal.dto;

/**
 * 修改密码响应。
 */
public class PortalPasswordChangeResponse {
    private final boolean success;
    private final boolean needRelogin;

    public PortalPasswordChangeResponse(boolean success, boolean needRelogin) {
        this.success = success;
        this.needRelogin = needRelogin;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isNeedRelogin() {
        return needRelogin;
    }
}
