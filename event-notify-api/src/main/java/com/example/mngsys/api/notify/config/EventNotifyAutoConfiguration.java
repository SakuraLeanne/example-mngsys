package com.example.mngsys.api.notify.config;

import com.example.mngsys.api.notify.core.EventNotifyPublisher;
import com.example.mngsys.api.notify.core.EventNotifySubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

/**
 * 自动装配 Redis Stream 相关的通用组件。
 */
@Configuration
@EnableConfigurationProperties(EventNotifyProperties.class)
public class EventNotifyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder();
        if (redisProperties.getTimeout() != null) {
            clientConfig.commandTimeout(redisProperties.getTimeout());
        }

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisProperties.getHost());
        serverConfig.setPort(redisProperties.getPort());
        serverConfig.setDatabase(redisProperties.getDatabase());
        String password = redisProperties.getPassword();
        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(RedisPassword.of(password));
        }

        return new LettuceConnectionFactory(serverConfig, clientConfig.build());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    public StreamMessageListenerContainer<String, MapRecord<String, String, Object>> streamMessageListenerContainer(
            StringRedisTemplate stringRedisTemplate) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, Object>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.<String, MapRecord<String, String, Object>>builder()
                        .batchSize(10)
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, Object>> container =
                StreamMessageListenerContainer.create(stringRedisTemplate.getRequiredConnectionFactory(), options);
        container.start();
        return container;
    }

    @Bean
    @ConditionalOnMissingBean
    public EventNotifyPublisher messagePublisher(StringRedisTemplate stringRedisTemplate, EventNotifyProperties properties) {
        return new EventNotifyPublisher(stringRedisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventNotifySubscriber messageSubscriber(StringRedisTemplate stringRedisTemplate,
                                                   StreamMessageListenerContainer<String, MapRecord<String, String, Object>> streamMessageListenerContainer,
                                                   EventNotifyProperties properties) {
        return new EventNotifySubscriber(stringRedisTemplate, streamMessageListenerContainer, properties);
    }
}
