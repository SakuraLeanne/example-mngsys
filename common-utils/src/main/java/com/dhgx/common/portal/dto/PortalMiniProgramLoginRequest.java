package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * PortalMiniProgramLoginRequest。
 * <p>
 * 微信小程序登录请求。
 * 小程序端先调用 wx.login() 获取 code，再传给门户后端换取 openId。
 * </p>
 */
public class PortalMiniProgramLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;

    /** 设备标识，可用于风控与会话治理。 */
    private String deviceId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
