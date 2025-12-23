package com.example.mngsys.redisevent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RedisEventNotifyProperties.class)
public class RedisEventNotifyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(StringRedisTemplate redisTemplate,
                                         RedisEventNotifyProperties properties) {
        return new RedisStreamEventPublisher(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventConsumerRunner eventConsumerRunner(StringRedisTemplate redisTemplate,
                                                   RedisEventNotifyProperties properties) {
        return new EventConsumerRunner(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PendingEventRetryer pendingEventRetryer(StringRedisTemplate redisTemplate,
                                                   RedisEventNotifyProperties properties) {
        return new PendingEventRetryer(redisTemplate, properties);
    }
}
