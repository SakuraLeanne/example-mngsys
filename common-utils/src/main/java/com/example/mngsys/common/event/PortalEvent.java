package com.example.mngsys.common.event;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * PortalEvent。
 */
public final class PortalEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final PortalEventType eventType;
    private final long userId;
    private final Long authVersion;
    private final Long profileVersion;
    private final Long operatorId;
    private final String operatorName;
    private final long ts;
    private final Map<String, Object> payload;

    private PortalEvent(String eventId,
                        PortalEventType eventType,
                        long userId,
                        Long authVersion,
                        Long profileVersion,
                        Long operatorId,
                        String operatorName,
                        long ts,
                        Map<String, Object> payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.userId = userId;
        this.authVersion = authVersion;
        this.profileVersion = profileVersion;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.ts = ts;
        this.payload = payload;
    }

    public static PortalEvent create(PortalEventType eventType,
                                     long userId,
                                     Long authVersion,
                                     Long profileVersion,
                                     Long operatorId,
                                     String operatorName,
                                     Map<String, Object> payload) {
        Objects.requireNonNull(eventType, "eventType");
        return new PortalEvent(
                UUID.randomUUID().toString(),
                eventType,
                userId,
                authVersion,
                profileVersion,
                operatorId,
                operatorName,
                System.currentTimeMillis(),
                payload
        );
    }

    public String getEventId() {
        return eventId;
    }

    public PortalEventType getEventType() {
        return eventType;
    }

    public long getUserId() {
        return userId;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public Long getProfileVersion() {
        return profileVersion;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public long getTs() {
        return ts;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
