package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * PortalAppWechatLoginRequest。
 * <p>
 * APP 微信授权登录请求，APP 端拉起微信授权后携带 code 调用门户。
 * </p>
 */
public class PortalAppWechatLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
