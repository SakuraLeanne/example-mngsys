package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * PortalMiniProgramPhoneLoginRequest。
 * <p>
 * 小程序“获取手机号并登录”请求。
 * 注意：phoneCode 来自 bindgetphonenumber 回调，与 wx.login 的 code 不同。
 * </p>
 */
public class PortalMiniProgramPhoneLoginRequest {

    @NotBlank(message = "phoneCode 不能为空")
    private String phoneCode;

    /** 设备标识，可用于风控与会话治理。 */
    private String deviceId;

    /** 目标系统编码，和 returnUrl 同时提供时会生成 SSO ticket。 */
    private String systemCode;

    /** 业务系统回调地址，和 systemCode 同时提供时会生成 SSO ticket。 */
    private String returnUrl;

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
