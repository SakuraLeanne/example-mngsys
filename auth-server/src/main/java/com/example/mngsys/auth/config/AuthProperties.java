package com.example.mngsys.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AuthProperties。
 * <p>
 * 认证中心配置属性，包含内部调用 Token 与短信配置项。
 * </p>
 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 内部调用的鉴权 Token。 */
    private String internalToken;
    /** 短信配置。 */
    private SmsProperties sms = new SmsProperties();
    /** 登录时是否自动创建用户。 */
    private boolean autoCreateUser = true;
    /** 密码重置配置。 */
    private PasswordResetProperties passwordReset = new PasswordResetProperties();
    /** 密码传输加密配置。 */
    private PasswordEncryptProperties passwordEncrypt = new PasswordEncryptProperties();

    /**
     * 获取内部 Token。
     */
    public String getInternalToken() {
        return internalToken;
    }

    /**
     * 设置内部 Token。
     */
    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    /**
     * 获取短信配置。
     */
    public SmsProperties getSms() {
        return sms;
    }

    /**
     * 设置短信配置。
     */
    public void setSms(SmsProperties sms) {
        this.sms = sms;
    }

    /**
     * 登录时是否自动创建用户。
     */
    public boolean isAutoCreateUser() {
        return autoCreateUser;
    }

    /**
     * 设置是否自动创建用户。
     */
    public void setAutoCreateUser(boolean autoCreateUser) {
        this.autoCreateUser = autoCreateUser;
    }

    /** 获取密码重置配置。 */
    public PasswordResetProperties getPasswordReset() {
        return passwordReset;
    }

    /** 设置密码重置配置。 */
    public void setPasswordReset(PasswordResetProperties passwordReset) {
        this.passwordReset = passwordReset;
    }

    /** 获取密码传输加密配置。 */
    public PasswordEncryptProperties getPasswordEncrypt() {
        return passwordEncrypt;
    }

    /** 设置密码传输加密配置。 */
    public void setPasswordEncrypt(PasswordEncryptProperties passwordEncrypt) {
        this.passwordEncrypt = passwordEncrypt;
    }

    /**
     * 短信配置。
     */
    public static class SmsProperties {
        /** AccessKeyId。 */
        private String accessKeyId;
        /** AccessKeySecret。 */
        private String accessKeySecret;
        /** 终端地址。 */
        private String endpoint = "dysmsapi.aliyuncs.com";
        /** 短信签名。 */
        private String signName;
        /** 模板编码集合。 */
        private TemplateCodeProperties templateCode = new TemplateCodeProperties();
        /** 验证码长度。 */
        private int codeLength = 6;
        /** 验证码有效期（秒）。 */
        private long ttlSeconds = 300;
        /** 发送间隔限制（秒）。 */
        private long sendIntervalSeconds = 60;

        /** 获取 AccessKeyId。 */
        public String getAccessKeyId() {
            return accessKeyId;
        }

        /** 设置 AccessKeyId。 */
        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        /** 获取 AccessKeySecret。 */
        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        /** 设置 AccessKeySecret。 */
        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        /** 获取 Endpoint。 */
        public String getEndpoint() {
            return endpoint;
        }

        /** 设置 Endpoint。 */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        /** 获取短信签名。 */
        public String getSignName() {
            return signName;
        }

        /** 设置短信签名。 */
        public void setSignName(String signName) {
            this.signName = signName;
        }

        /** 获取模板编码。 */
        public TemplateCodeProperties getTemplateCode() {
            return templateCode;
        }

        /** 设置模板编码。 */
        public void setTemplateCode(TemplateCodeProperties templateCode) {
            this.templateCode = templateCode;
        }

        /** 获取验证码长度。 */
        public int getCodeLength() {
            return codeLength;
        }

        /** 设置验证码长度。 */
        public void setCodeLength(int codeLength) {
            this.codeLength = codeLength;
        }

        /** 获取有效期。 */
        public long getTtlSeconds() {
            return ttlSeconds;
        }

        /** 设置有效期。 */
        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        /** 获取发送间隔。 */
        public long getSendIntervalSeconds() {
            return sendIntervalSeconds;
        }

        /** 设置发送间隔。 */
        public void setSendIntervalSeconds(long sendIntervalSeconds) {
            this.sendIntervalSeconds = sendIntervalSeconds;
        }

        /**
         * 模板编码配置。
         */
        public static class TemplateCodeProperties {
            /** 注册/通用验证码模板。 */
            private String verificationCode;
            /** 登录验证码模板。 */
            private String loginVerificationCode;

            public String getVerificationCode() {
                return verificationCode;
            }

            public void setVerificationCode(String verificationCode) {
                this.verificationCode = verificationCode;
            }

            public String getLoginVerificationCode() {
                return loginVerificationCode;
            }

            public void setLoginVerificationCode(String loginVerificationCode) {
                this.loginVerificationCode = loginVerificationCode;
            }
        }
    }

    /**
     * 密码重置相关配置。
     */
    public static class PasswordResetProperties {
        /** 重置令牌有效期（秒）。 */
        private long tokenTtlSeconds = 600;
        /** 重置令牌发放频控（秒）。 */
        private long issueIntervalSeconds = 60;
        /** 令牌校验最大失败次数。 */
        private int maxVerifyFailures = 5;

        public long getTokenTtlSeconds() {
            return tokenTtlSeconds;
        }

        public void setTokenTtlSeconds(long tokenTtlSeconds) {
            this.tokenTtlSeconds = tokenTtlSeconds;
        }

        public long getIssueIntervalSeconds() {
            return issueIntervalSeconds;
        }

        public void setIssueIntervalSeconds(long issueIntervalSeconds) {
            this.issueIntervalSeconds = issueIntervalSeconds;
        }

        public int getMaxVerifyFailures() {
            return maxVerifyFailures;
        }

        public void setMaxVerifyFailures(int maxVerifyFailures) {
            this.maxVerifyFailures = maxVerifyFailures;
        }
    }

    /**
     * 密码传输加密配置。
     */
    public static class PasswordEncryptProperties {
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
}
