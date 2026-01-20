package com.dhgx.common.security;

/**
 * 密码传输加密配置。
 */
public class PasswordEncryptProperties {
    /** 是否开启加密传输校验。 */
    private boolean enabled = false;
    /**
     * AES 密钥（长度建议 16/24/32 字节），用于解密前端传输的密码。
     * 为安全起见，应通过配置中心下发。
     */
    private String aesKey;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
