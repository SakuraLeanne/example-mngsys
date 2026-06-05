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
    /** 单点登录跳转地址。 */
    private String jumpUrl;
    /** 单点登录票据。 */
    private String ticket;

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
        return loginSuccess(accessToken, refreshToken, expiresIn, userId, null, null);
    }

    public static PortalMobileLoginResponse loginSuccess(String accessToken,
                                                         String refreshToken,
                                                         Long expiresIn,
                                                         String userId,
                                                         String jumpUrl,
                                                         String ticket) {
        PortalMobileLoginResponse response = new PortalMobileLoginResponse();
        response.setBindRequired(false);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        response.setUserId(userId);
        response.setJumpUrl(jumpUrl);
        response.setTicket(ticket);
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

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
