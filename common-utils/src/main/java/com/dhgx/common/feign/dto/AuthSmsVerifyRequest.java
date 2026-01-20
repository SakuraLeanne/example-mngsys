package com.dhgx.common.feign.dto;

import javax.validation.constraints.NotBlank;

/**
 * AuthSmsVerifyRequest。
 */
public class AuthSmsVerifyRequest {
    @NotBlank(message = "手机号不能为空")
    private String mobile;
    @NotBlank(message = "验证码不能为空")
    private String code;

    public AuthSmsVerifyRequest() {
    }

    public AuthSmsVerifyRequest(String mobile, String code) {
        this.mobile = mobile;
        this.code = code;
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
