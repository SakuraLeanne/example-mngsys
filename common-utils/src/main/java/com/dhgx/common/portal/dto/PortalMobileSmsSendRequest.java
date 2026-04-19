package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * PortalMobileSmsSendRequest。
 * <p>
 * 移动端发送登录验证码请求。
 * </p>
 */
public class PortalMobileSmsSendRequest {

    @NotBlank(message = "mobile 不能为空")
    private String mobile;

    @NotNull(message = "clientType 不能为空")
    private PortalMobileClientType clientType;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public PortalMobileClientType getClientType() {
        return clientType;
    }

    public void setClientType(PortalMobileClientType clientType) {
        this.clientType = clientType;
    }
}
