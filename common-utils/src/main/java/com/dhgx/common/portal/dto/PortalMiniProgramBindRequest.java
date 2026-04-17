package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * PortalMiniProgramBindRequest。
 * <p>
 * 微信小程序首次登录时，绑定手机号请求。
 * </p>
 */
public class PortalMiniProgramBindRequest {

    @NotBlank(message = "bindToken 不能为空")
    private String bindToken;

    @NotBlank(message = "mobile 不能为空")
    private String mobile;

    @NotBlank(message = "code 不能为空")
    private String code;

    public String getBindToken() {
        return bindToken;
    }

    public void setBindToken(String bindToken) {
        this.bindToken = bindToken;
    }

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
}
