package com.example.mngsys.common.feign.dto;

/**
 * AuthLoginRequest。
 */
public class AuthLoginRequest {
    private String mobile;
    private String code;

    public AuthLoginRequest() {
    }

    public AuthLoginRequest(String mobile, String code) {
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
