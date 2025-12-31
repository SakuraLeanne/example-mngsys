package com.example.mngsys.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码加密配置，使用 DelegatingPasswordEncoder 统一管理算法。
 * 默认算法为 Argon2id，兼容 bcrypt，所有哈希均带算法前缀以避免明文或可逆加密。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 配置密码编码器，默认使用 Argon2id，兼容 bcrypt。
     *
     * @return DelegatingPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        String defaultId = "argon2";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(defaultId, new Argon2PasswordEncoder());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        return new DelegatingPasswordEncoder(defaultId, encoders);
    }
}
