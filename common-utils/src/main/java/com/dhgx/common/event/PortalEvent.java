package com.dhgx.common.event;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * PortalEvent。
 * <p>
 * 门户侧用户相关事件的标准数据结构，支持事件唯一标识、事件类型、操作者信息、
 * 以及业务载荷，用于事件通知与审计。通过静态工厂方法创建，自动生成事件 ID
 * 和时间戳，避免业务方重复编码。
 * </p>
 */
public final class PortalEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 事件唯一标识，使用 UUID 生成。 */
    private final String eventId;
    /** 事件类型。 */
    private final PortalEventType eventType;
    /** 关联的用户 ID（字符串形式，兼容雪花/UUID）。 */
    private final String userId;
    /** 当前授权数据版本，用于增量消费。 */
    private final Long authVersion;
    /** 当前档案数据版本，用于增量消费。 */
    private final Long profileVersion;
    /** 操作人 ID，系统任务可为空。 */
    private final String operatorId;
    /** 操作人姓名或账户名，便于审计展示。 */
    private final String operatorName;
    /** 事件生成时间戳（毫秒）。 */
    private final long ts;
    /** 事件携带的业务扩展数据。 */
    private final Map<String, Object> payload;

    private PortalEvent(String eventId,
                        PortalEventType eventType,
                        String userId,
                        Long authVersion,
                        Long profileVersion,
                        String operatorId,
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
                                     String userId,
                                     Long authVersion,
                                     Long profileVersion,
                                     String operatorId,
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

    public String getUserId() {
        return userId;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public Long getProfileVersion() {
        return profileVersion;
    }

    public String getOperatorId() {
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
