package com.example.mngsys.api.notify.core;

import com.example.mngsys.api.notify.config.EventNotifyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.util.Assert;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class EventNotifySubscriber {
    private final StringRedisTemplate stringRedisTemplate;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final EventNotifyProperties properties;

    /**
     * 注册一个消费者监听，消费组不存在时会自动创建。
     *
     * @param handler 消息处理逻辑
     * @return 订阅句柄，可在需要时取消订阅
     */
    public Subscription subscribe(EventNotifyHandler handler) {
        return subscribe(properties.getStreamKey(), properties.getConsumerGroup(), properties.getConsumerName(), handler);
    }

    /**
     * 指定 Stream Key 与消费组的订阅方式，便于不同功能点隔离消息流。
     *
     * @param streamKey     需要订阅的 Stream Key
     * @param consumerGroup 消费组名称
     * @param consumerName  消费者名称
     * @param handler       消息处理逻辑
     * @return 订阅句柄，可在需要时取消订阅
     */
    public Subscription subscribe(String streamKey, String consumerGroup, String consumerName, EventNotifyHandler handler) {
        Assert.hasText(streamKey, "streamKey must not be blank");
        Assert.hasText(consumerGroup, "consumerGroup must not be blank");
        Assert.hasText(consumerName, "consumerName must not be blank");
        createGroupIfNecessary(streamKey, consumerGroup);
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(consumerGroup, consumerName);

        return container.receive(consumer, streamOffset, new StreamListener<String, MapRecord<String, String, String>>() {
            @Override
            public void onMessage(MapRecord<String, String, String> message) {
                Map<String, String> body = message.getValue();
                handler.onMessage(message.getId().getValue(), body);
            }
        });
    }

    private void createGroupIfNecessary(String streamKey, String consumerGroup) {
        if (!properties.isCreateGroupIfAbsent()) {
            return;
        }
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, consumerGroup);
            log.info("创建消费组 {} 用于 Stream {}", consumerGroup, streamKey);
        } catch (RedisSystemException e) {
            // 消费组已存在时会抛出 BUSYGROUP，忽略即可
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("消费组 {} 已存在", consumerGroup);
            } else {
                throw e;
            }
        }
    }
}
