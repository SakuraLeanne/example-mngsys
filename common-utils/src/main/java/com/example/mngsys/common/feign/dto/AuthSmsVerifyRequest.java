package com.example.mngsys.common.feign.dto;

/**
 * AuthSmsVerifyRequest。
 */
public class AuthSmsVerifyRequest {
    private String mobile;
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
