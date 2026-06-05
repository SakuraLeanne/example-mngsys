package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * PortalMobileSmsLoginRequest。
 * <p>
 * 移动端手机号验证码登录请求。
 * </p>
 */
public class PortalMobileSmsLoginRequest {

    @NotBlank(message = "mobile 不能为空")
    private String mobile;

    @NotBlank(message = "code 不能为空")
    private String code;

    /** 设备标识，可用于风控与会话治理。 */
    private String deviceId;

    @NotNull(message = "clientType 不能为空")
    private PortalMobileClientType clientType;

    /** 目标系统编码，和 returnUrl 同时提供时会生成 SSO ticket。 */
    private String systemCode;

    /** 业务系统回调地址，和 systemCode 同时提供时会生成 SSO ticket。 */
    private String returnUrl;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

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

    public PortalMobileClientType getClientType() {
        return clientType;
    }

    public void setClientType(PortalMobileClientType clientType) {
        this.clientType = clientType;
    }
}
