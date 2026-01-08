package com.example.mngsys.api.notify.core;

import com.example.mngsys.api.notify.config.EventNotifyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public class EventNotifyPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final EventNotifyProperties properties;

    /**
     * 发布单字段消息，常用于简单事件通知。
     *
     * @param fieldName 字段名，例如 "event"
     * @param body      字段值，例如 "user.created"
     * @return Redis 生成的 RecordId
     */
    public RecordId publish(String fieldName, String body) {
        return publish(Collections.singletonMap(fieldName, body));
    }

    /**
     * 发布多字段消息，便于携带更丰富的业务数据。
     *
     * @param message 消息字段集合
     * @return Redis 生成的 RecordId
     */
    public RecordId publish(Map<String, String> message) {
        return publish(properties.getStreamKey(), message);
    }

    /**
     * 使用指定的 Stream Key 发布单字段消息，便于按功能隔离事件。
     *
     * @param streamKey 自定义的 Stream Key
     * @param fieldName 字段名
     * @param body      字段值
     * @return Redis 生成的 RecordId
     */
    public RecordId publishTo(String streamKey, String fieldName, String body) {
        return publish(streamKey, Collections.singletonMap(fieldName, body));
    }

    /**
     * 使用指定的 Stream Key 发布多字段消息。
     *
     * @param streamKey 自定义的 Stream Key
     * @param message   消息字段集合
     * @return Redis 生成的 RecordId
     */
    public RecordId publish(String streamKey, Map<String, String> message) {
        if (CollectionUtils.isEmpty(message)) {
            throw new IllegalArgumentException("message payload must not be empty");
        }
        Assert.hasText(streamKey, "streamKey must not be blank");
        MapRecord<String, String, String> record = StreamRecords.mapBacked(message)
                .withStreamKey(streamKey);
        return stringRedisTemplate.opsForStream().add(record);
    }
}
