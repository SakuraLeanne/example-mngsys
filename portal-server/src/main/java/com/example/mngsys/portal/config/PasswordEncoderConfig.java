package com.example.mngsys.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码加密配置。
 * <p>
 * 使用 DelegatingPasswordEncoder 统一管理密码哈希算法：
 * 默认采用 Argon2id，兼容 bcrypt，所有哈希都会带上算法前缀（如 {argon2} / {bcrypt}），
 * 杜绝明文或可逆加密存储。
 * </p>
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 提供 DelegatingPasswordEncoder Bean，默认 Argon2id，兼容 bcrypt。
     *
     * @return PasswordEncoder
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
