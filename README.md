# dhgx-portal

## 项目简介

统一门户，包含认证服务、门户服务、网关路由与事件通知能力。通过 Nacos 进行服务发现与配置管理，网关统一路由与认证校验。


## 登录方式

认证服务（`auth-server`）目前支持三种登录方式的协议，其中前两种已实现：

- **用户名密码登录**：校验用户名与密码，用户不存在会返回错误，不会自动创建。
- **手机号验证码登录**：先调用短信发送接口获取验证码，登录时校验验证码；如手机号用户不存在且配置 `auth.auto-create-user=true`（默认）会自动创建用户。
- **二维码登录**：预留扩展能力，暂未实现。

## 网关（gateway-server）

### Nacos 路由配置示例

`portal-share-config.yml`：

```yaml
spring:
    cloud:
      gateway:
        routes:
          - id: auth-server
            uri: lb://auth-server
            predicates:
              # 匹配前端访问 /authserver/** 的请求
              - Path=/authserver/**
            filters:
              # 去掉前缀 /authserver
              - StripPrefix=1
              # 转发时在路径前追加后端 context-path /auth-server
              - PrefixPath=/auth-server
  
          - id: portal-server
            uri: lb://portal-server
            predicates:
              # 匹配前端访问 /portalserver/** 的请求
              - Path=/portalserver/**
            filters:
              # 去掉前缀 /authserver
              - StripPrefix=1
              # 转发在路径前追加后端 context-path /portal-server
              - PrefixPath=/portal-server
```

### 白名单配置

`portal-share-config.yml`：

```yaml
gateway:
  security:
    whitelist:
      - /portalserver/login/**
      - /portalserver/password/forgot/**
      - /portalserver/password/change
      - /portalserver/profile
      - /portalserver/action/**
      - /portalserver/test/**
      - /portalserver/captcha/image
      - /portalserver/app/menus
      - /portalserver/sso/ticket/verify
```
## 错误码梳理（按接口分类）

> 说明：
>
> - 本节基于当前代码实现整理，响应结构统一为 `{"code": number, "message": string, "data": any}`。
> - `code=0` 表示成功；以下均为失败态错误码。
> - 认证服务主要使用 `auth-server` 的基础错误码；门户与网关使用 `portal-server/common-utils` 的扩展错误码。

### 1) 全局错误码总览

| 错误码 | 默认 HTTP 状态 | 错误描述 | 典型业务场景 |
| --- | --- | --- | --- |
| `100100` | `401` | 登录已失效，请先登录 | 未携带登录凭证、会话失效、Token 版本不一致、内部调用鉴权失败 |
| `100200` | `403` | 暂无访问权限，请联系管理员 | 管理端接口缺少管理员权限、角色/权限校验不通过 |
| `100300` | `400` | 请求参数有误，请检查后重试 | DTO 校验失败、参数缺失/格式错误、业务前置条件不满足 |
| `100301` | `400` | 回调地址不合法或不在白名单 | SSO 跳转 `returnUrl/targetUrl` 不在允许域名内 |
| `100310` | `400` | 请先完成验证码校验 | 用户名密码登录启用图形验证码后未提供 `captchaId/captchaCode` |
| `100311` | `400` | 验证码无效，请重新获取 | 图形验证码不存在、过期或校验失败 |
| `100320` | `401` | 账号或密码错误 | 门户用户名密码登录失败时统一脱敏返回 |
| `100404` | `404` | 资源不存在或已被删除 | 用户/角色/菜单不存在 |
| `100500` | `500` | 系统开小差了，请稍后再试 | 未捕获异常、下游服务异常或无响应 |
| `200110` | `410` | 校验票据无效，请重新发起操作 | action ticket 不存在、格式错误、类型不匹配 |
| `200111` | `410` | 校验票据已过期，请重新发起操作 | action ticket 已过期 |
| `200112` | `409` | 校验票据已被使用，请重新发起操作 | action ticket 重放或重复消费 |
| `200120` | `401` | 登录凭证无效，请重新登录 | `ptk` 无效或无法解析用户 |
| `200121` | `410` | 登录凭证已过期，请重新登录 | `ptk` 过期 |
| `200122` | `403` | 登录凭证权限不匹配，请确认访问范围 | `ptk` scope 与当前动作不一致 |
| `300100` | `403` | 账号已被停用，请联系管理员 | 用户状态非启用（停用/冻结） |
| `300110` | `400` | 旧密码不正确，请重试 | 修改密码时旧密码校验失败 |
| `300111` | `400` | 新密码不符合安全策略，请修改后重试 | 新密码不满足复杂度规则 |
| `400210` | `400` | 票据无效或已过期，请重新发起登录 | SSO ticket 不存在、过期、格式非法 |
| `400211` | `403` | 业务系统不匹配，请确认 `systemCode` 是否正确 | ticket 绑定系统与当前验证系统不一致 |
| `400212` | `403` | 回跳地址不匹配，请检查 `redirectUri` | ticket 绑定回跳地址与当前请求不一致 |
| `400213` | `409` | 状态校验失败，请重新发起登录 | ticket 绑定 state 与当前 state 不一致 |
| `400214` | `429` | 请求过于频繁，请稍后再试 | SSO ticket 验证触发限流 |
| `400215` | `500` | 系统异常，请稍后再试 | SSO ticket 脚本结果异常、票据载荷解析失败等 |

### 2) 网关鉴权接口（gateway）

#### 2.1 `gateway -> /portal/api/**` 全局过滤

- 路径不在白名单且未携带 `satoken`：返回 `100100`（未登录）。
- 携带 `satoken` 但调用认证服务会话校验失败/异常：返回 `100100`（未登录）。
- 网关会尽量透传认证服务返回的 message，便于前端区分“凭证缺失/过期/无效”等细分原因。

### 3) 认证服务接口（auth-server）

#### 3.1 登录与短信能力

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `POST /auth/api/login` | `100300` | 不支持登录类型、用户名/密码缺失、验证码错误、用户不存在、用户冻结/停用等参数或业务校验失败 |
|  | `100500` | 登录过程出现未处理异常 |
| `POST /auth/api/login/sms/send` | `100300` | 手机号不合法、发送频率过高 |
| `POST /auth/api/login/sms/verify` | `100300` | 验证码未发送/过期/错误 |

#### 3.2 忘记密码

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `POST /auth/api/password/forgot/send` | `100300` | 手机号非法、短信发送过频 |
| `POST /auth/api/password/forgot/verify` | `100300` | 验证码错误或过期 |
| `POST /auth/api/password/forgot/reset` | `100300` | resetToken 缺失/过期/不匹配、密码参数不合法、用户不存在 |

#### 3.3 会话管理

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `GET /auth/api/session-info` | `100100` | 未登录、会话失效、token 版本校验失败 |
| `POST /auth/api/logout` | - | 正常返回成功（当前实现未显式返回业务错误码） |
| `POST /auth/api/session/kick` | `100100` | `X-Internal-Token` 校验失败（内部调用未授权） |

### 4) 门户用户侧接口（portal-server）

#### 4.1 登录与验证码

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `POST /portal/api/login` | `100310` | 启用图形验证码后未传 `captchaId/captchaCode` |
|  | `100311` | 图形验证码错误、过期或已失效 |
|  | `100320` | 用户名密码登录失败（对外统一返回“账号或密码错误”） |
|  | `100300` | 登录参数错误（如登录方式不支持、字段缺失） |
|  | `100100` | 短信登录鉴权失败或会话无效 |
|  | `100500` | 下游 auth 无响应/系统异常 |
| `GET /portal/api/captcha/image` | `100500` | 验证码配置缺失或生成图片失败 |
| `POST /portal/api/login/sms/send` | `100300` | 手机号参数非法、发送频控 |
|  | `100500` | 下游 auth 无响应 |

#### 4.2 忘记密码

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `POST /portal/api/password/forgot/send` | `100300` | 手机号非法或频率限制 |
| `POST /portal/api/password/forgot/verify` | `100300` | 验证码错误/过期 |
| `POST /portal/api/password/forgot/reset` | `100300` | resetToken 不匹配、密码参数错误 |
| 上述三类接口 | `100500` | 下游 auth 无响应 |

#### 4.3 登录态与个人中心

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `GET /portal/api/loginuser/session-info` | `100100` | 无登录上下文 |
|  | `100404` | 用户不存在 |
|  | `300100` | 用户被停用 |
| `POST /portal/api/password/change` | `100100` | 缺失 `ptk/satoken` 或登录态无效 |
|  | `100404` | 用户不存在 |
|  | `300100` | 用户状态非启用 |
|  | `300110` | 旧密码校验失败 |
|  | `300111` | 新密码不满足复杂度策略 |
|  | `100300` | 明文/密文字段不完整或解密后为空 |
| `GET /portal/api/profile` | `100100` / `100404` / `300100` | 未登录、用户不存在、用户停用 |
| `POST /portal/api/profile` | `100100` / `100404` / `300100` / `100300` | 未登录、用户不存在、用户停用、昵称/邮箱等参数不合法 |

#### 4.4 敏感动作票据（Action Ticket）

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `POST /portal/api/action/pwd/enter` | `200110` | ticket 不存在/类型不匹配/非法 |
|  | `200111` | ticket 过期 |
|  | `200112` | ticket 已被消费（重放） |
| `POST /portal/api/action/profile/enter` | 同上 | 与密码动作入口一致 |
| `GET /test/action-ticket/pwd` | `100300` / `100301` / `100404` / `300100` | `userId<=0`、`returnUrl` 为空等参数校验失败；回跳地址不在白名单；用户不存在或已禁用 |
| `GET /test/action-ticket/profile` | `100300` / `100301` / `100404` / `300100` | `userId<=0`、`returnUrl` 为空等参数校验失败；回跳地址不在白名单；用户不存在或已禁用 |

### 5) 门户 SSO 接口（portal-server）

| 接口 | 主要错误码 | 业务场景 |
| --- | --- | --- |
| `POST /portal/api/sso/jump-url` | `100100` | 未登录无法签发 ticket |
|  | `100301` | `targetUrl` 不在白名单或格式非法 |
| `POST /portal/api/sso/ticket/verify` | `400210` | ticket 无效/过期、参数缺失，或票据未携带可用会话信息（如 `tokenValue` 缺失） |
|  | `400211` | `systemCode` 与票据绑定系统不一致 |
|  | `400212` | `redirectUri` 与票据绑定地址不一致 |
|  | `400213` | `state` 校验失败（防重放状态不一致） |
|  | `400214` | 触发频率限制 |
|  | `400215` | Redis 脚本执行异常、票据载荷解析失败、用户不存在等系统级问题 |
| `POST /portal/api/sso/ticket/logout` | `400210` | `gSessionId`/`logoutToken` 无效、缺失，或全局会话不存在 |
|  | `400211` | `systemCode` 与该 `gSessionId` 绑定系统不一致 |
|  | `400215` | 全局会话映射数据损坏等系统级问题 |

### 6) 门户后台管理接口（admin）

后台接口集中在 `/portal/api/admin/**`，不同资源控制器错误码风格一致，主要包括：

- `100200`：无管理员权限（如菜单/角色/用户管理动作）。
- `100300`：参数错误或业务冲突（如角色编码重复、菜单路径重复、菜单存在子节点不可删、角色停用不可授权）。
- `100404`：目标用户/角色/菜单不存在。

典型接口：

- 用户管理：`/admin/users`。
- 角色管理：`/admin/app-roles`。
- 菜单管理：`/admin/app-menus`。
- 用户-角色授权：`/admin/app-users/{userId}/roles`、`/admin/app-roles/{roleId}/users`。

### 7) 全局异常到错误码映射规则

- 参数校验异常（`@Valid`/约束校验/`IllegalArgumentException`）统一映射为 `100300`。
- 鉴权异常（未登录）映射为 `100100`；权限异常映射为 `100200`。
- 业务异常（`LocalizedBusinessException`）按异常携带的错误码返回。
- 未捕获异常统一映射为 `100500`。

> 建议前端处理策略：
>
> - `401/410`：引导重新登录或重新发起动作；
> - `403`：提示无权限并提供联系管理员入口；
> - `409`：提示并阻止重复提交；
> - `429`：展示节流提示与重试倒计时；
> - `500`：统一“系统繁忙”并记录 `traceId` 便于排障。

