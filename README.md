# example-mngsys

## 项目简介

示例管理系统，包含认证服务、门户服务、网关与 Redis 事件通知能力。通过 Nacos 进行服务发现与配置管理，网关统一路由与认证校验，门户服务提供 RBAC 与菜单下发能力，认证服务负责登录与会话校验。

## 技术基线

- Java 8
- Spring Boot 2.3.8.RELEASE
- Spring Cloud Hoxton.SR12
- Spring Cloud Alibaba 2.2.7.RELEASE
- MyBatis-Plus（门户服务）
- Redis（会话与事件流）
- Nacos（服务发现/配置）

## 服务清单

| 服务 | 说明 | 端口 | 主要职责 |
| --- | --- | --- | --- |
| `auth-server` | 认证服务 | 8080（默认） | 登录、会话校验、踢人等基础鉴权能力 |
| `portal-server` | 门户服务 | 8081（默认） | RBAC 管理、菜单下发、用户管理、审计日志 |
| `gateway-server` | 网关服务 | 8082（默认） | 统一路由、白名单放行、登录态校验 |
| `redis-event-notify-api` | 事件通知组件 | N/A | 基于 Redis Stream 的事件发布/消费能力 |
| `common-utils` | 通用模块 | N/A | 通用 API 响应、错误码与 Redis Key 约定 |

> 端口以各服务 `application.yml` 为准。

## 认证登录方式

认证服务（`auth-server`）目前支持三种登录方式的协议，其中前两种已实现：

- **用户名密码登录**：校验用户名与明文密码，用户不存在会返回错误，不会自动创建。
- **手机号验证码登录**：先调用短信发送接口获取验证码，登录时校验验证码；如手机号用户不存在且配置 `auth.auto-create-user=true`（默认）会自动创建用户。
- **手机号扫码二维码登录**：预留扩展能力，当前接口会返回“暂未支持二维码登录”。

### 接口调用示例（curl）

1. 发送短信验证码
   ```bash
   curl -X POST http://localhost:8080/auth/api/sms/send \
     -H 'Content-Type: application/json' \
     -d '{"mobile":"13800000001"}'
   ```

2. 手机号验证码登录（默认方式）
   ```bash
   curl -X POST http://localhost:8080/auth/api/login \
     -H 'Content-Type: application/json' \
     -d '{"mobile":"13800000001","code":"123456"}'
   ```

3. 用户名密码登录
   ```bash
   curl -X POST http://localhost:8080/auth/api/login \
     -H 'Content-Type: application/json' \
     -d '{"loginType":"USERNAME_PASSWORD","username":"admin","password":"admin123456"}'
   ```

4. 二维码登录占位（当前会返回未支持错误）
   ```bash
   curl -X POST http://localhost:8080/auth/api/login \
     -H 'Content-Type: application/json' \
     -d '{"loginType":"QR_CODE","mobile":"13800000001"}'
   ```

## 构建说明

- 根目录新增聚合 `pom.xml`，统一管理各模块版本，可直接执行 `mvn -DskipTests package` 进行多模块构建。
- 通用能力已抽取为 `common-utils` 模块，后续可在其他服务中以 Maven 依赖方式复用。

## 网关（gateway-server）

### Nacos 路由配置示例

`gateway-server/src/main/resources/nacos-routes-example.yml`：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-server
          uri: lb://auth-server
          predicates:
            - Path=/auth/**
        - id: portal-server
          uri: lb://portal-server
          predicates:
            - Path=/portal/**
```

### 白名单配置

`gateway-server/src/main/resources/application.yml`：

```yaml
gateway:
  security:
    whitelist:
      - /auth/api/login
      - /auth/api/sms/send
      - /portal/api/login
      - /portal/api/sms/send
      - /portal/api/action/**
```

#### 白名单设计逻辑

- **入口职责**：白名单由网关统一校验，只有命中白名单的路径才会跳过登录态校验，其余请求会先调用认证服务 `/auth/api/session/me` 校验。
- **最小暴露**：仅登录、验证码发送、动作票据入口等必须匿名的接口应该被加入白名单；业务查询、修改接口不应放行。
- **粒度控制**：支持精确路径与 Ant 风格通配（如 `/portal/api/action/**`）。建议优先使用精确路径，通配仅用于同一动作前缀的入口。
- **配置优先级**：本地配置可覆盖 Nacos 中的白名单（如需集中管理，可在 Nacos 配置新增 `gateway.security.whitelist`）。

#### 如何配置白名单

1. **本地配置文件**：在 `gateway-server/src/main/resources/application.yml` 下的 `gateway.security.whitelist` 数组中添加/删除路径。
2. **Nacos 配置中心**（可选）：若通过 Nacos 统一管理网关配置，在对应的 `gateway-server` 配置文件中添加相同层级的 `gateway.security.whitelist`。
3. **重启/热更新**：
   - 本地修改需要重启 `gateway-server` 生效。
   - 若开启了 Nacos `refresh-enabled: true`，则在 Nacos 更新后会自动刷新。

### Portal 登录态校验 Filter

网关在 `PortalAuthGlobalFilter` 中对 `/portal/api/**`（除白名单）调用 `GET /auth/api/session/me` 校验登录态，并透传 Cookie。失败则返回 `401`，响应体 `code=100100`。

## redis-event-notify-api 能力与用法

### 能力说明

- **事件发布**：`EventPublisher` 将事件写入 Redis Stream（默认 `portal:events`）。
- **事件消费**：`EventConsumerRunner` 以 Consumer Group 形式拉取并派发事件，支持去重与确认 ACK。
- **处理分发**：`EventDispatcher` 支持多 `EventHandler` 分发执行。
- **失败重试**：`PendingEventRetryer` 支持处理 Pending 消息重试。

### 使用方式

1. 引入依赖（示例见 `auth-server`/`portal-server`）：
   ```xml
   <dependency>
       <groupId>com.example.mngsys</groupId>
       <artifactId>redis-event-notify-api</artifactId>
       <version>1.0.0-SNAPSHOT</version>
   </dependency>
   ```
2. 配置 `RedisEventNotifyProperties`（如 `streamKey`、`groupName`、`consumerName`）。
3. 发布事件：
   ```java
   EventMessage message = new EventMessage();
   message.setEventId("...");
   message.setEventType("USER_DISABLED");
   eventPublisher.publish(message);
   ```
4. 消费事件：实现 `EventHandler`，并通过 `EventConsumerRunner#consumeOnce` 拉取并处理。

## 其他说明

- `portal-server` 中包含 RBAC 管理接口与菜单下发逻辑，支持 Redis 缓存与审计日志。
- `auth-server` 为登录与会话校验入口，`gateway-server` 统一路由与校验。
- 如需本地联调，请确保 Redis 与 Nacos 可用，并启动各服务。
