package com.dhgx.common.feign.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * AuthSmsSendRequest。
 */
public class AuthSmsSendRequest {
    /** 手机号。 */
    @NotBlank(message = "手机号不能为空")
    private String mobile;
    /** 短信场景。 */
    @NotNull(message = "短信场景不能为空")
    private AuthSmsScene scene = AuthSmsScene.LOGIN;

    public AuthSmsSendRequest() {
    }

    public AuthSmsSendRequest(String mobile, AuthSmsScene scene) {
        this.mobile = mobile;
        this.scene = scene;
    }

    public static AuthSmsSendRequest loginScene(String mobile) {
        return new AuthSmsSendRequest(mobile, AuthSmsScene.LOGIN);
    }

    public static AuthSmsSendRequest verificationScene(String mobile) {
        return new AuthSmsSendRequest(mobile, AuthSmsScene.VERIFICATION);
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public AuthSmsScene getScene() {
        return scene;
    }

    public void setScene(AuthSmsScene scene) {
        this.scene = scene;
    }

    public AuthSmsScene getSceneOrDefault() {
        return scene == null ? AuthSmsScene.LOGIN : scene;
    }
}
