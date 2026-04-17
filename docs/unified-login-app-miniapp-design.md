# 门户统一登录接入设计（APP + 微信小程序）

## 1. 背景与目标

当前系统登录现状：

- 门户（portal）作为统一登录入口，已支持：用户名密码、手机号验证码、预留扫码。  
- 门户调用 auth-server 完成认证，auth-server 使用 Sa-Token 建立会话，并返回 `satoken`。  
- 业务系统通过 `ticket` 向门户换取登录态，再在各自系统签发内部 Token。  

新增诉求：

- 新增 **手机 APP** 与 **微信小程序** 接入门户统一登录。
- 登录方式：
  - APP：微信授权登录、手机号验证码登录。
  - 小程序：微信登录、手机号验证码登录。
- 对 APP/小程序要求签发 `access_token`（不直接依赖浏览器 Cookie）。
- 门户用户主表 `portal_user` 无微信绑定字段，需扩展关联模型。

本设计目标：

1. 复用现有 portal/auth 架构，最小侵入演进。
2. 保持“门户统一身份源”，完成账号绑定与用户归一。
3. 面向移动端输出标准化 `access_token` + `refresh_token`。
4. 与现有 SSO ticket 模型兼容，不影响已有业务系统被动登录链路。

---

## 2. 现状梳理（基于现有代码）

### 2.1 已有登录类型与能力

- 登录类型枚举仅包含 `SMS / USERNAME_PASSWORD / QR_CODE`，无微信类型。  
  位置：`AuthLoginType`。  
- auth-server 的 `/login` 按登录类型分支；`QR_CODE` 当前明确抛出“暂未支持二维码登录”。
- portal 的 `/login` 透传到 auth-server，成功后返回 `satoken`，并在有 `systemCode+returnUrl` 时签发 SSO ticket 跳转地址。

### 2.2 用户模型

- `portal_user` 同时被 portal-server 与 auth-server 作为用户认证/资料基础表使用。
- 认证实体 `AuthUser` 映射同一张 `portal_user`。
- 目前没有第三方身份（微信 unionid/openid）字段。

### 2.3 网关鉴权现状

- gateway 只检查 Cookie 中是否存在 `satoken`，并调用 auth-server `/session-info`。
- 未支持 `Authorization: Bearer access_token`。

### 2.4 结论

移动端要接入统一登录，核心缺口在于：

1. 第三方身份（微信）与门户用户缺少映射关系。
2. 现有会话输出形态偏浏览器 Cookie（`satoken`），不适配 APP/小程序。
3. 鉴权链路缺少“移动端 access_token 校验”能力。

---

## 3. 总体方案

采用“双层令牌”架构：

- **认证会话层（内部）**：继续使用 Sa-Token（`satoken`）作为统一会话主状态，保证与现有门户与 SSO ticket 机制兼容。
- **客户端令牌层（外部）**：面向 APP/小程序签发 `access_token` 与 `refresh_token`（建议 opaque token + Redis 存储）。

即：

1. 用户在 APP/小程序完成微信或短信登录。
2. 门户内部仍通过 auth-server 建立/复用 Sa-Token 会话。
3. 门户新增“移动端 token 服务”，基于登录用户签发 access_token。
4. 网关新增 Bearer Token 校验逻辑（优先 Bearer，其次 Cookie）。

---

## 4. 数据模型设计

> 由于 `portal_user` 需保持稳定，不建议直接塞入多平台微信字段，采用“一主表 + 身份绑定子表”模式。

### 4.1 新表：`portal_user_identity`

```sql
CREATE TABLE portal_user_identity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL COMMENT 'portal_user.id',
  identity_provider VARCHAR(32) NOT NULL COMMENT 'WECHAT_OPEN_PLATFORM/WECHAT_MINI_PROGRAM/MOBILE',
  identity_type VARCHAR(32) NOT NULL COMMENT 'UNIONID/OPENID/MOBILE',
  identity_key VARCHAR(128) NOT NULL COMMENT '如 unionid/openid/手机号(脱敏或哈希)',
  app_id VARCHAR(64) DEFAULT NULL COMMENT '微信appId/小程序appId',
  tenant_id VARCHAR(64) DEFAULT NULL,
  verified TINYINT NOT NULL DEFAULT 1,
  bind_status TINYINT NOT NULL DEFAULT 1 COMMENT '1-已绑定 0-解绑',
  bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  unbind_time DATETIME DEFAULT NULL,
  ext_json VARCHAR(1024) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_provider_type_key (identity_provider, identity_type, identity_key),
  KEY idx_user_provider (user_id, identity_provider),
  CONSTRAINT fk_identity_user FOREIGN KEY (user_id) REFERENCES portal_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户用户外部身份绑定表';
```

### 4.2 设计要点

- “归一主键”是 `user_id`，所有身份都汇聚到 portal_user。
- 微信侧建议优先用 `unionid` 归一（同一开放平台下跨应用唯一），`openid` 作为应用内标识补充。
- 手机号身份也可入此表，便于统一“身份绑定视图”。

---

## 5. 令牌模型设计

### 5.1 access_token 建议

- 类型：opaque 随机串（不直接暴露用户信息，便于服务端吊销）。
- 存储：Redis。
- TTL：2 小时（可配置）。
- 绑定信息：
  - userId
  - clientType（APP / MINI_PROGRAM）
  - deviceId（可选）
  - satoken（或 satoken hash + tokenVersion）
  - scope
  - issueTime / expireTime

### 5.2 refresh_token 建议

- 类型：长随机串，仅用于换新 access_token。
- TTL：30 天（可配置）。
- 单设备单 refresh_token（建议），支持主动下线与轮换。
- Redis key 建议：
  - `PORTAL:MOBILE:AT:{accessToken}`
  - `PORTAL:MOBILE:RT:{refreshToken}`

### 5.3 与 Sa-Token 的关系

- access_token 是“客户端通行证”；satoken 是“认证会话源”。
- 网关校验 access_token 后，把 `X-User-Id`、`X-Client-Type` 注入下游。
- 若 satoken 失效（登出/踢下线/版本失效），access_token 校验应同步失败（通过 tokenVersion 或回源校验实现）。

### 5.4 Opaque Token 与 OAuth2 + OIDC 的取舍

你提到的点非常关键：**完全可以采用 OAuth2 + OIDC**，并不是只能 opaque token。

两者关系不是“二选一协议 vs 字符串”，而是：

- OAuth2/OIDC 是授权与身份标准体系（流程、端点、声明规范）。
- Opaque/JWT 是 access_token 的具体形态（令牌格式）。

也就是说，可以有以下组合：

1. OAuth2/OIDC + opaque token（配 introspection）
2. OAuth2/OIDC + JWT access token（资源端本地验签）
3. 非 OAuth2/OIDC + 自定义 opaque token（本方案初稿）

#### 当前项目下的差异

- 采用“自定义 opaque”：
  - 优点：改造快、与当前 satoken 体系耦合成本低。
  - 缺点：对外不是标准协议，后续接第三方生态（网关产品、统一 IAM、开放平台）会有迁移成本。
- 采用“OAuth2 + OIDC”：
  - 优点：标准化、可扩展、便于未来接入更多客户端/合作方系统。
  - 成本：需要新增标准端点、client 管理、scope/claim 体系、JWKS/introspection、授权码+PKCE 等能力。

#### 建议落地策略（推荐）

采用“**分阶段标准化**”：

1. **短期（快速上线）**：保留 opaque + Redis，但接口尽量向 OAuth2 命名对齐（如 `/oauth/token`、`/oauth/introspect` 语义）。
2. **中期（标准化）**：引入 OAuth2 授权服务器能力（可自研轻量版或引入 Spring Authorization Server），先支持移动端关键流程（授权码+PKCE、刷新令牌）。
3. **长期（统一身份平台）**：补齐 OIDC（`id_token`、`userinfo`、`jwks_uri`、`/.well-known/openid-configuration`），让 APP/小程序/Web/第三方系统统一接入。

#### 结合你们现阶段的明确结论

如果你们希望“一次到位、避免二次重构”，本项目可以直接按 **OAuth2.1 + OIDC Core** 目标设计。
若当前优先级是“尽快上线 APP/小程序登录”，则先落地 opaque 方案，再按上面的中期路径平滑升级。

---

## 6. 登录与绑定流程设计

## 6.1 APP 微信授权登录

1. APP 获取微信授权码（code）并提交门户。
2. 门户调用微信开放平台换取 `openid/unionid`。
3. 查询 `portal_user_identity`：
   - 找到已绑定用户 → 直接登录。
   - 未找到 → 返回“需绑定手机号”状态与一次性 `bind_token`。
4. 若需绑定，APP 再调用手机号验证码绑定接口：
   - 校验短信验证码。
   - 按手机号查询 portal_user（不存在则按策略创建）。
   - 写入微信绑定关系。
5. 调用 auth-server 建立 Sa-Token 会话。
6. 门户签发 access_token + refresh_token 返回给 APP。

## 6.2 小程序微信登录

流程与 APP 基本一致，差异点：

- 使用 `jscode2session` 获取 `openid/session_key`，若可得 unionid 则优先 unionid。
- `identity_provider` 区分为 `WECHAT_MINI_PROGRAM`。

## 6.3 手机号验证码登录（APP/小程序）

1. 调用发送验证码（复用现有 auth-server 短信服务）。
2. 校验验证码登录（复用现有 SMS 认证）。
3. 登录成功后由门户签发 access_token + refresh_token。
4. 若该用户存在待补全的微信绑定上下文，可提示是否绑定（非阻塞登录）。

---

## 7. API 设计（建议稿）

> 不直接改现有 `/portal/api/login` 的浏览器语义，新增 `/portal/api/mobile/**` 端点，降低兼容风险。

### 7.1 登录相关

1. `POST /portal/api/mobile/login/sms/send`
2. `POST /portal/api/mobile/login/sms`
3. `POST /portal/api/mobile/login/wechat/app`
4. `POST /portal/api/mobile/login/wechat/mini-program`
5. `POST /portal/api/mobile/bind/wechat-mobile`

### 7.2 Token 相关

1. `POST /portal/api/mobile/token/refresh`
2. `POST /portal/api/mobile/token/logout`
3. `GET /portal/api/mobile/token/introspect`（网关内部调用或内网接口）

### 7.3 统一响应体（建议）

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "access_token": "...",
    "token_type": "Bearer",
    "expires_in": 7200,
    "refresh_token": "...",
    "refresh_expires_in": 2592000,
    "user": {
      "user_id": "u-xxx",
      "mobile": "138****0000",
      "real_name": "张三"
    },
    "bind_required": false,
    "bind_token": null
  }
}
```

---

## 8. 网关与下游改造点

### 8.1 网关鉴权顺序

1. 若有 `Authorization: Bearer xxx`：走移动 access_token 校验。
2. 否则回退现有 Cookie `satoken` 校验。
3. 两者都无则返回未登录。

### 8.2 下游透传头

- `X-User-Id`
- `X-Client-Type`（WEB/APP/MINI_PROGRAM）
- `X-Auth-Source`（SATOKEN/ACCESS_TOKEN）

### 8.3 与现有业务系统 ticket 模式关系

- **不替换**原 ticket 被动登录模式。
- APP/小程序是“门户直签 access_token”；业务系统 Web 继续“ticket 换本地 token”。
- 后续可增补“移动端换业务系统 token”接口，但非本期必须。

---

## 9. 安全设计

1. **微信 code 一次性消费**：防重放（Redis 记录 code 或 nonce）。
2. **bind_token 短时有效**：建议 5 分钟，仅用于绑定流程。
3. **敏感数据脱敏与加密存储**：identity_key 可按类型加密/哈希。
4. **设备维度风控**：记录 deviceId、ip、ua，支持异常登录告警。
5. **会话联动失效**：用户改密/禁用时，satoken 与 access_token 一并失效。
6. **最小权限**：小程序/APP 的 access_token scope 默认仅基础业务 scope。

---

## 10. 与现有代码的映射关系（实施导向）

### 10.1 auth-server

- 扩展 `AuthLoginType`（新增 WECHAT_APP / WECHAT_MINI_PROGRAM，或保留 SMS+新增 mobile 入口由 portal 自行编排）。
- 更建议：**不入侵 auth-server 登录枚举**，微信鉴权前置在 portal，auth-server 仍只负责“按 userId 建立会话”或“手机号登录”。
- 若按现状最小改动，可新增 auth 内部接口：`/session/login-by-user-id`（仅 internal-token 可调）。

### 10.2 portal-server

- 新增 identity 领域：entity/mapper/service/controller。
- 新增 mobile token 领域：token 签发、刷新、吊销、校验。
- 新增微信网关 client（开放平台 & 小程序）。
- 复用现有 `AuthClient` 与 `PortalUserService` 做用户主数据读写。

### 10.3 gateway-server

- 在 `PortalAuthGlobalFilter` 增加 Bearer 解析与 introspect 调用。
- 保留原 cookie satoken 逻辑作为兼容分支。

---

## 11. 分阶段落地计划

### Phase 1（最小可用）

- 建表 `portal_user_identity`。
- 实现 APP/小程序短信登录并返回 access_token。
- 网关支持 Bearer access_token 鉴权。
- 保持现有 Web 登录与 ticket 不变。

### Phase 2（微信打通）

- 接入微信 code 换取身份。
- 实现“未绑定手机号 -> 绑定 -> 登录”闭环。
- 完成 unionid 归一与冲突处理策略。

### Phase 3（会话治理增强）

- 设备管理、单设备下线、多端会话策略。
- 登录风控策略（频控、异地、异常设备）。
- 统一登出联动（satoken / access_token / gSession）。

---

## 12. 决策建议（本轮评审结论）

建议采用以下确定方案：

1. **用户归一**：新增 `portal_user_identity` 表，不改 `portal_user` 主体结构。
2. **登录编排归口 portal**：微信登录逻辑放在 portal，auth-server继续作为基础会话服务。
3. **移动令牌独立**：新增 access_token/refresh_token，不直接暴露 satoken 给 APP/小程序；令牌体系建议分阶段演进到 OAuth2 + OIDC 标准。
4. **网关双通道兼容**：Bearer + Cookie 并存，逐步迁移。
5. **保持现有 SSO ticket 机制稳定**：不与移动端 token 方案互相替代。

该方案对现有线上影响最小，扩展性最好，也最符合“统一门户登录 + 多终端接入”的目标。
