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
        List<PendingMessage> messages = pending.getMessages();
        List<MapRecord<String, String, String>> claimed = redisTemplate.opsForStream()
                .claim(streamKey, properties.getGroupName(), properties.getConsumerName(), minIdleTime, messages);
        List<EventMessage> handled = new ArrayList<>();
        if (claimed == null) {
            return handled;
        }
        for (MapRecord<String, String, String> record : claimed) {
            EventMessage message = toMessage(record.getValue());
            boolean success = dispatcher.handle(message);
            if (success) {
                redisTemplate.opsForStream().acknowledge(streamKey, properties.getGroupName(), record.getId());
                handled.add(message);
            }
        }
        return handled;
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
