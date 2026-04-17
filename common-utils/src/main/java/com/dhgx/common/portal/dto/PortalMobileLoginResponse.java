package com.dhgx.common.portal.dto;

/**
 * PortalMobileLoginResponse。
 * <p>
 * 移动端统一登录返回体，支持“已登录”与“待绑定手机号”两种状态。
 * </p>
 */
public class PortalMobileLoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private boolean bindRequired;
    private String bindToken;
    private String userId;

    public static PortalMobileLoginResponse bindRequired(String bindToken) {
        PortalMobileLoginResponse response = new PortalMobileLoginResponse();
        response.setBindRequired(true);
        response.setBindToken(bindToken);
        return response;
    }

    public static PortalMobileLoginResponse loginSuccess(String accessToken,
                                                         String refreshToken,
                                                         Long expiresIn,
                                                         String userId) {
        PortalMobileLoginResponse response = new PortalMobileLoginResponse();
        response.setBindRequired(false);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        response.setUserId(userId);
        return response;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public boolean isBindRequired() {
        return bindRequired;
    }

    public void setBindRequired(boolean bindRequired) {
        this.bindRequired = bindRequired;
    }

    public String getBindToken() {
        return bindToken;
    }

    public void setBindToken(String bindToken) {
        this.bindToken = bindToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
