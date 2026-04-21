package com.dhgx.portal.config;

import com.dhgx.common.security.PasswordEncryptProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
/**
 * AuthClientProperties。
 */
public class AuthClientProperties {

    private String internalToken;
    /**
     * 是否允许自动创建用户，与 auth-server 的 auth.auto-create-user 配置保持一致。
     */
    private boolean autoCreateUser = false;
    private PasswordEncryptProperties passwordEncrypt = new PasswordEncryptProperties();

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public boolean isAutoCreateUser() {
        return autoCreateUser;
    }

    public void setAutoCreateUser(boolean autoCreateUser) {
        this.autoCreateUser = autoCreateUser;
    }

    public PasswordEncryptProperties getPasswordEncrypt() {
        return passwordEncrypt;
    }

    public void setPasswordEncrypt(PasswordEncryptProperties passwordEncrypt) {
        this.passwordEncrypt = passwordEncrypt;
    }
}
