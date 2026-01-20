package com.dhgx.portal.config;

import com.dhgx.common.security.PasswordCryptoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PasswordCryptoConfig。
 */
@Configuration
public class PasswordCryptoConfig {

    @Bean
    public PasswordCryptoService passwordCryptoService(AuthClientProperties authClientProperties) {
        return new PasswordCryptoService(authClientProperties.getPasswordEncrypt());
    }
}
