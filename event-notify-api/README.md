# event-notify-api 模块说明

`event-notify-api` 是一个基于 **Redis Stream** 的轻量级事件通知模块。

---

## 1. 模块能力

### 1.1 自动装配能力
引入该模块后，Spring Boot 会自动装配以下组件：

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

## 2. 引入方式

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

## 3. 配置说明

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

---

## 4. 使用示例

### 4.1 发布事件

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

### 4.2 订阅事件

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

---



