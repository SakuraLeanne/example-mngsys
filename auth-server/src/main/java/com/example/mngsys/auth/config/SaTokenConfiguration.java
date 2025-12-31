package com.example.mngsys.auth.config;

import cn.dev33.satoken.config.SaCookieConfig;
import cn.dev33.satoken.config.SaTokenConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SaTokenConfiguration。
 * <p>
 * 配置 Sa-Token 的基本参数与 Cookie 设置。
 * </p>
 */
@Configuration
public class SaTokenConfiguration {

    /**
     * 配置 Sa-Token。
     *
     * @return SaTokenConfig 实例
     */
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
