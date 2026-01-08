package com.example.mngsys.api.notify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Stream 配置项，便于各微服务统一调整流名称、消费组与消费者标识。
 */
@Data
@ConfigurationProperties(prefix = "portal.redis-stream")
public class EventNotifyProperties {
    /**
     * 统一的 Stream Key，所有事件都写入该 Stream。
     */
    private String streamKey = "dhgx:stream:events";

    /**
     * 消费组名称，用于区分不同的业务消费侧。
     */
    private String consumerGroup = "default-consumer-group";

    /**
     * 消费者名称，同组内唯一，通常建议使用服务名或实例标识。
     */
    private String consumerName = "default-consumer";

    /**
     * 当消费组不存在时，是否自动创建，便于开箱即用。
     */
    private boolean createGroupIfAbsent = true;

}
