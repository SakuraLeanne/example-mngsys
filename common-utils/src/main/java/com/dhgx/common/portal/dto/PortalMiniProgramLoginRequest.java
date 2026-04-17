package com.dhgx.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * PortalMiniProgramLoginRequest。
 * <p>
 * 微信小程序登录请求。当前阶段先接收 openId，后续可替换为 code 换取 openId。
 * </p>
 */
public class PortalMiniProgramLoginRequest {

    @NotBlank(message = "openId 不能为空")
    private String openId;

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }
}
