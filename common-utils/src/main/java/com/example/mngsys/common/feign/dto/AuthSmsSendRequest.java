package com.example.mngsys.common.feign.dto;

/**
 * AuthSmsSendRequest。
 */
public class AuthSmsSendRequest {
    private String mobile;
    private String scene;

    public AuthSmsSendRequest() {
    }

    public AuthSmsSendRequest(String mobile, String scene) {
        this.mobile = mobile;
        this.scene = scene;
    }

    public static AuthSmsSendRequest loginScene(String mobile) {
        return new AuthSmsSendRequest(mobile, "LOGIN");
    }

    public static AuthSmsSendRequest verificationScene(String mobile) {
        return new AuthSmsSendRequest(mobile, "VERIFICATION");
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }
}
