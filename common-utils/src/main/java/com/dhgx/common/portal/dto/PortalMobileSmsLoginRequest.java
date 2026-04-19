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

    @NotNull(message = "clientType 不能为空")
    private PortalMobileClientType clientType;

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

    public PortalMobileClientType getClientType() {
        return clientType;
    }

    public void setClientType(PortalMobileClientType clientType) {
        this.clientType = clientType;
    }
}
