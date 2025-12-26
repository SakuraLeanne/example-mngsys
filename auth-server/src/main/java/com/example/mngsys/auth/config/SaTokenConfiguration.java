package com.example.mngsys.auth.config;

import cn.dev33.satoken.config.SaCookieConfig;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

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

    @Bean
    public SaTokenDao saTokenDao(RedisTemplate<String, Object> redisTemplate) {
        SaTokenDaoRedisJackson dao = new SaTokenDaoRedisJackson();
        dao.setRedisTemplate(redisTemplate);
        return dao;
    }
}
