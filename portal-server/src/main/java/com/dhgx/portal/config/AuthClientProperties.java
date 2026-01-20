package com.dhgx.portal.config;

import com.dhgx.common.security.PasswordEncryptProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
/**
 * AuthClientProperties。
 */
public class AuthClientProperties {

    private String internalToken;
    private PasswordEncryptProperties passwordEncrypt = new PasswordEncryptProperties();

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public PasswordEncryptProperties getPasswordEncrypt() {
        return passwordEncrypt;
    }

    public void setPasswordEncrypt(PasswordEncryptProperties passwordEncrypt) {
        this.passwordEncrypt = passwordEncrypt;
    }
}
