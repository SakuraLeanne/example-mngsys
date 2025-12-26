package com.example.mngsys.redisevent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * PendingEventRetryer。
 */
public class PendingEventRetryer {
    private final StringRedisTemplate redisTemplate;
    private final RedisEventNotifyProperties properties;

    public PendingEventRetryer(StringRedisTemplate redisTemplate, RedisEventNotifyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public List<EventMessage> retryPending(EventDispatcher dispatcher, Duration minIdleTime, int count) {
        String streamKey = properties.getStreamKey();
        PendingMessages pending = redisTemplate.opsForStream()
                .pending(streamKey, properties.getGroupName(), Range.unbounded(), count);
        if (pending == null || pending.isEmpty()) {
            return new ArrayList<>();
        }
        List<PendingMessage> messages = new ArrayList<>();
        pending.forEach(messages::add);
        String[] ids = messages.stream()
                .map(p -> p.getId().getValue())
                .toArray(String[]::new);
        List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream()
                .claim(streamKey, properties.getGroupName(), properties.getConsumerName(), minIdleTime.toMillis(), ids);
        List<EventMessage> handled = new ArrayList<>();
        if (claimed == null) {
            return handled;
        }
        for (MapRecord<String, Object, Object> record : claimed) {
            EventMessage message = toMessage(asStringObjectMap(record.getValue()));
            boolean success = dispatcher.handle(message);
            if (success) {
                redisTemplate.opsForStream().acknowledge(streamKey, properties.getGroupName(), record.getId());
                handled.add(message);
            }
        }
        return handled;
    }

    private Map<String, Object> asStringObjectMap(Map<Object, Object> source) {
        Map<String, Object> target = new java.util.HashMap<>();
        if (source != null) {
            for (Map.Entry<Object, Object> entry : source.entrySet()) {
                target.put(asString(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    private EventMessage toMessage(Map<String, Object> map) {
        EventMessage message = new EventMessage();
        message.setEventId(asString(map.get("eventId")));
        message.setEventType(asString(map.get("eventType")));
        message.setUserId(parseLong(map.get("userId")));
        message.setAuthVersion(parseLong(map.get("authVersion")));
        message.setProfileVersion(parseLong(map.get("profileVersion")));
        message.setOperatorId(parseLong(map.get("operatorId")));
        message.setOperatorName(asString(map.get("operatorName")));
        message.setTs(parseLong(map.get("ts")));
        message.setPayload(asString(map.get("payload")));
        return message;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long parseLong(Object value) {
        String string = asString(value);
        if (!StringUtils.hasText(string)) {
            return null;
        }
        return Long.valueOf(string);
    }
}
