package com.example.mngsys.portal.config;

import com.example.mngsys.portal.common.PortalActionTicketUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * PortalActionTicketUtil 配置。
 */
@Configuration
public class PortalActionTicketConfig {

    /**
     * 构建 PortalActionTicketUtil 实例。
     */
    @Bean
    public PortalActionTicketUtil portalActionTicketUtil(StringRedisTemplate redisTemplate,
                                                         ObjectMapper objectMapper,
                                                         PortalProperties portalProperties,
                                                         @Value("${portal.action.portal-base-url:https://portal.example.com}") String portalBaseUrl,
                                                         @Value("${portal.action.source-system-code:portal-server}") String sourceSystemCode,
                                                         @Value("${portal.action.ticket-ttl-seconds:300}") long ttlSeconds) {
        Set<String> allowedHosts = new HashSet<>(portalProperties.getSecurity().getAllowedHosts());
        return new PortalActionTicketUtil(redisTemplate, objectMapper, portalBaseUrl, sourceSystemCode, allowedHosts, ttlSeconds);
    }
}
