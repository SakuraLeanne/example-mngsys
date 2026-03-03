package com.dhgx.common.portal.dto;

/**
 * SSO ticket 校验成功响应。
 */
public class PortalSsoTicketLoginResponse {
    private final String userId;
    private final String username;
    private final String mobile;
    private final String realName;
    /** 登录时间（格式：yyyy-MM-dd HH:mm:ss）。 */
    private final String loginTime;
    /** 全局会话 ID。 */
    private final String gSessionId;
    /** 单点登出凭证。 */
    private final String logoutToken;
    /** 全局会话到期时间（格式：yyyy-MM-dd HH:mm:ss）。 */
    private final String expireAt;

    public PortalSsoTicketLoginResponse(String userId,
                                        String username,
                                        String mobile,
                                        String realName,
                                        String loginTime,
                                        String gSessionId,
                                        String logoutToken,
                                        String expireAt) {
        this.userId = userId;
        this.username = username;
        this.mobile = mobile;
        this.realName = realName;
        this.loginTime = loginTime;
        this.gSessionId = gSessionId;
        this.logoutToken = logoutToken;
        this.expireAt = expireAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getMobile() {
        return mobile;
    }

    public String getRealName() {
        return realName;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public String getGSessionId() {
        return gSessionId;
    }

    public String getLogoutToken() {
        return logoutToken;
    }

    public String getExpireAt() {
        return expireAt;
    }
}
