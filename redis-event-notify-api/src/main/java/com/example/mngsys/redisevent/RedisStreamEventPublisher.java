package com.example.mngsys.redisevent;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

public class RedisStreamEventPublisher implements EventPublisher {
    private final StringRedisTemplate redisTemplate;
    private final RedisEventNotifyProperties properties;

    public RedisStreamEventPublisher(StringRedisTemplate redisTemplate, RedisEventNotifyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public String publish(String streamKey, EventMessage message) {
        String targetStream = StringUtils.hasText(streamKey) ? streamKey : properties.getStreamKey();
        Map<String, String> body = new HashMap<>();
        if (message.getEventId() != null) {
            body.put("eventId", message.getEventId());
        }
        if (message.getEventType() != null) {
            body.put("eventType", message.getEventType());
        }
        if (message.getUserId() != null) {
            body.put("userId", message.getUserId().toString());
        }
        if (message.getAuthVersion() != null) {
            body.put("authVersion", message.getAuthVersion().toString());
        }
        if (message.getProfileVersion() != null) {
            body.put("profileVersion", message.getProfileVersion().toString());
        }
        if (message.getOperatorId() != null) {
            body.put("operatorId", message.getOperatorId().toString());
        }
        if (message.getOperatorName() != null) {
            body.put("operatorName", message.getOperatorName());
        }
        if (message.getTs() != null) {
            body.put("ts", message.getTs().toString());
        }
        if (message.getPayload() != null) {
            body.put("payload", message.getPayload());
        }
        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord().in(targetStream).ofMap(body));
        return recordId != null ? recordId.getValue() : null;
    }
}
