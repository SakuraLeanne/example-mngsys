package com.example.mngsys.auth.config;

import com.example.mngsys.common.security.PasswordCryptoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PasswordCryptoConfig。
 */
@Configuration
public class PasswordCryptoConfig {

    @Bean
    public PasswordCryptoService passwordCryptoService(AuthProperties authProperties) {
        return new PasswordCryptoService(authProperties.getPasswordEncrypt());
    }
}
