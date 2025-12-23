package com.example.mngsys.redisevent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

public class EventConsumerRunner {
    private final StringRedisTemplate redisTemplate;
    private final RedisEventNotifyProperties properties;

    public EventConsumerRunner(StringRedisTemplate redisTemplate, RedisEventNotifyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public List<EventMessage> consumeOnce(EventDispatcher dispatcher) {
        String streamKey = properties.getStreamKey();
        ensureGroup(streamKey, properties.getGroupName());

        Consumer consumer = Consumer.from(properties.getGroupName(), properties.getConsumerName());
        StreamReadOptions options = StreamReadOptions.empty()
                .count(properties.getBatchSize())
                .block(Duration.ofMillis(properties.getBlockMillis()));
        List<MapRecord<String, String, String>> records = redisTemplate.opsForStream()
                .read(consumer, options, StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
        List<EventMessage> handled = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return handled;
        }
        for (MapRecord<String, String, String> record : records) {
            EventMessage message = toMessage(record.getValue());
            String eventId = message.getEventId();
            if (StringUtils.hasText(eventId)) {
                String dedupKey = buildDedupKey(properties.getSystemCode(), eventId);
                Boolean first = redisTemplate.opsForValue().setIfAbsent(
                        dedupKey,
                        "1",
                        Duration.ofSeconds(properties.getDedupTtlSeconds())
                );
                if (Boolean.FALSE.equals(first)) {
                    redisTemplate.opsForStream().acknowledge(streamKey, properties.getGroupName(), record.getId());
                    continue;
                }
            }
            boolean success = dispatcher.handle(message);
            if (success) {
                redisTemplate.opsForStream().acknowledge(streamKey, properties.getGroupName(), record.getId());
                handled.add(message);
            }
        }
        return handled;
    }

    private void ensureGroup(String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), groupName);
        } catch (RedisSystemException ex) {
            String message = ex.getMessage();
            if (message == null || !message.contains("BUSYGROUP")) {
                throw ex;
            }
        }
    }

    private String buildDedupKey(String systemCode, String eventId) {
        return properties.getDedupKeyPrefix() + systemCode + ":" + eventId;
    }

    private EventMessage toMessage(Map<String, String> map) {
        EventMessage message = new EventMessage();
        message.setEventId(map.get("eventId"));
        message.setEventType(map.get("eventType"));
        message.setUserId(parseLong(map.get("userId")));
        message.setAuthVersion(parseLong(map.get("authVersion")));
        message.setProfileVersion(parseLong(map.get("profileVersion")));
        message.setOperatorId(parseLong(map.get("operatorId")));
        message.setOperatorName(map.get("operatorName"));
        message.setTs(parseLong(map.get("ts")));
        message.setPayload(map.get("payload"));
        return message;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Long.valueOf(value);
    }
}
