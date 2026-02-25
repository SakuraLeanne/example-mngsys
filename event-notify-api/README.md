# event-notify-api 模块说明

`event-notify-api` 是一个基于 **Redis Stream** 的轻量级事件通知模块，面向 Spring Boot 项目提供“开箱即用”的事件发布与订阅能力，适用于服务间异步解耦、状态变更广播、业务通知等场景。

---

## 1. 模块能力

### 1.1 自动装配能力
引入该模块后，Spring Boot 会自动装配以下组件（可按需覆盖）：

- `RedisConnectionFactory`（当业务未自行定义时自动创建）
- `StringRedisTemplate`
- `StreamMessageListenerContainer`
- `EventNotifyPublisher`（事件发布器）
- `EventNotifySubscriber`（事件订阅器）

自动装配入口：`META-INF/spring.factories`。

### 1.2 事件发布能力（Publisher）
`EventNotifyPublisher` 提供了多种发布方式：

- 单字段发布：`publish(fieldName, body)`
- 多字段发布：`publish(Map<String, String> message)`
- 指定 Stream 发布：`publishTo(streamKey, fieldName, body)`
- 指定 Stream + 多字段：`publish(streamKey, message)`

返回值为 Redis 的 `RecordId`，可用于日志追踪与排障。

### 1.3 事件订阅能力（Subscriber）
`EventNotifySubscriber` 提供消费组订阅能力：

- 默认参数订阅：`subscribe(handler)`
- 自定义 `streamKey / consumerGroup / consumerName`：`subscribe(streamKey, group, consumer, handler)`

并支持：

- 当 `createGroupIfAbsent=true` 时自动创建消费组
- 通过函数式接口 `EventNotifyHandler` 接收消息（`messageId` + `body`）

---

## 2. 适用场景

- 用户注册/审批/支付成功后的异步通知
- 业务事件广播（如“工单创建”“任务完成”）
- 跨模块解耦（生产者不依赖消费者实现）
- 简单事件总线能力建设

---

## 3. 引入方式

在使用方模块中添加依赖：

```xml
<dependency>
  <groupId>com.dhgx.api.notify</groupId>
  <artifactId>event-notify-api</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> 该模块已依赖 `spring-boot-starter-data-redis`，使用前请确保运行环境可连接 Redis。

---

## 4. 配置说明

模块使用 `portal.redis-stream` 作为配置前缀：

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    # password: your-password

portal:
  redis-stream:
    stream-key: dhgx:stream:events
    consumer-group: portal-group
    consumer-name: portal-consumer-01
    create-group-if-absent: true
```

配置项说明：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `portal.redis-stream.stream-key` | `dhgx:stream:events` | 默认事件流 Key |
| `portal.redis-stream.consumer-group` | `default-consumer-group` | 默认消费组 |
| `portal.redis-stream.consumer-name` | `default-consumer` | 默认消费者名（建议实例维度唯一） |
| `portal.redis-stream.create-group-if-absent` | `true` | 消费组不存在时是否自动创建 |

---

## 5. 使用示例

### 5.1 发布事件

```java
import com.dhgx.api.notify.core.EventNotifyPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserEventService {
    private final EventNotifyPublisher eventNotifyPublisher;

    public void publishUserCreated(Long userId) {
        eventNotifyPublisher.publish(Map.of(
                "event", "user.created",
                "userId", String.valueOf(userId),
                "source", "portal-server"
        ));
    }
}
```

### 5.2 订阅事件

```java
import com.dhgx.api.notify.core.EventNotifySubscriber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final EventNotifySubscriber eventNotifySubscriber;

    @PostConstruct
    public void init() {
        eventNotifySubscriber.subscribe((messageId, body) -> {
            String event = body.get("event");
            if ("user.created".equals(event)) {
                log.info("收到用户创建事件, messageId={}, body={}", messageId, body);
                // TODO: 执行业务处理
            }
        });
    }
}
```

### 5.3 多流隔离订阅（可选）

```java
eventNotifySubscriber.subscribe(
    "dhgx:stream:orders",
    "order-group",
    "order-consumer-01",
    (messageId, body) -> {
        // 处理订单事件
    }
);
```

---

## 6. 运行与排查建议

1. **消费者命名建议唯一化**：同一消费组内建议按“服务名 + 实例标识”命名，避免定位困难。  
2. **流与组的命名建议业务化**：如 `dhgx:stream:orders` / `order-group`，便于运维识别。  
3. **消息体字段约定统一**：建议固定字段（如 `event`、`traceId`、`timestamp`）提升可观测性。  
4. **启动顺序建议**：先确保 Redis 可用，再启动消费者服务，减少组创建/订阅异常。  
5. **消息确认策略**：当前订阅实现未显式 `ACK`，如需严格控制待确认消息（PEL），建议结合业务扩展确认与重试机制。  

---

## 7. 对外 API 一览

- `com.dhgx.api.notify.core.EventNotifyPublisher`
- `com.dhgx.api.notify.core.EventNotifySubscriber`
- `com.dhgx.api.notify.core.EventNotifyHandler`
- `com.dhgx.api.notify.config.EventNotifyProperties`

