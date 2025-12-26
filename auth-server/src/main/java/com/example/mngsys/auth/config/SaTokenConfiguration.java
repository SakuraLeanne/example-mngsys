package com.example.mngsys.auth.config;

import cn.dev33.satoken.config.SaCookieConfig;
import cn.dev33.satoken.config.SaTokenConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * SaTokenConfiguration。
 */
public class SaTokenConfiguration {

    @Bean
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        config.setTokenName("satoken");
        SaCookieConfig cookieConfig = new SaCookieConfig();
        cookieConfig.setHttpOnly(true);
        config.setCookie(cookieConfig);
        return config;
    }

}
