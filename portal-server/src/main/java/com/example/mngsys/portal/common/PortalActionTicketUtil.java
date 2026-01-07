package com.example.mngsys.portal.common;

import com.example.mngsys.portal.common.exception.InvalidReturnUrlException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Portal Action Ticket 工具类。
 */
public class PortalActionTicketUtil {
    /** 修改密码 action_ticket 前缀。 */
    private static final String ACTION_TICKET_PASSWORD_PREFIX = "portal:action:ticket:pwd:";
    /** 个人资料 action_ticket 前缀。 */
    private static final String ACTION_TICKET_PROFILE_PREFIX = "portal:action:ticket:profile:";
    /** 默认 TTL 秒数。 */
    private static final long DEFAULT_TTL_SECONDS = 300L;

    private static final Logger log = LoggerFactory.getLogger(PortalActionTicketUtil.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String portalBaseUrl;
    private final String sourceSystemCode;
    private final Set<String> allowedHosts;
    private final long ttlSeconds;

    /**
     * 构造函数，默认 TTL 300 秒。
     *
     * @param redisTemplate    Redis 操作模板
     * @param objectMapper     JSON 序列化器
     * @param portalBaseUrl    门户基础地址，例如 https://portal.xxx.com
     * @param sourceSystemCode 来源系统编码
     * @param allowedHosts     returnUrl 白名单 host 集合
     */
    public PortalActionTicketUtil(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  String portalBaseUrl,
                                  String sourceSystemCode,
                                  Set<String> allowedHosts) {
        this(redisTemplate, objectMapper, portalBaseUrl, sourceSystemCode, allowedHosts, DEFAULT_TTL_SECONDS);
    }

    /**
     * 构造函数。
     *
     * @param redisTemplate    Redis 操作模板
     * @param objectMapper     JSON 序列化器
     * @param portalBaseUrl    门户基础地址，例如 https://portal.xxx.com
     * @param sourceSystemCode 来源系统编码
     * @param allowedHosts     returnUrl 白名单 host 集合
     * @param ttlSeconds       action_ticket TTL 秒数
     */
    public PortalActionTicketUtil(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  String portalBaseUrl,
                                  String sourceSystemCode,
                                  Set<String> allowedHosts,
                                  long ttlSeconds) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper不能为空");
        if (!StringUtils.hasText(portalBaseUrl)) {
            throw new IllegalArgumentException("portalBaseUrl不能为空");
        }
        if (!StringUtils.hasText(sourceSystemCode)) {
            throw new IllegalArgumentException("sourceSystemCode不能为空");
        }
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            throw new IllegalArgumentException("allowedHosts不能为空");
        }
        this.portalBaseUrl = portalBaseUrl;
        this.sourceSystemCode = sourceSystemCode;
        this.allowedHosts = allowedHosts;
        this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;
    }

    /**
     * 生成修改密码 action_ticket 并返回门户跳转链接。
     * <p>
     * Redis Key: portal:action:ticket:pwd:{ticket}，TTL 默认 300 秒。
     * Value 字段：userId、sourceSystemCode、returnUrl、issuedAt、expireAt、nonce。
     * </p>
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return 门户跳转 URL
     */
    public String createPwdChangeJumpUrl(long userId, String returnUrl) {
        String ticket = createPwdTicket(userId, returnUrl);
        return buildJumpUrl("/portal/password/change", ticket);
    }

    /**
     * 生成个人信息维护 action_ticket 并返回门户跳转链接。
     * <p>
     * Redis Key: portal:action:ticket:profile:{ticket}，TTL 默认 300 秒。
     * Value 字段：userId、sourceSystemCode、returnUrl、issuedAt、expireAt、nonce。
     * </p>
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return 门户跳转 URL
     */
    public String createProfileEditJumpUrl(long userId, String returnUrl) {
        String ticket = createProfileTicket(userId, returnUrl);
        return buildJumpUrl("/portal/profile/edit", ticket);
    }

    /**
     * 生成修改密码 action_ticket。
     * <p>
     * Redis Key: portal:action:ticket:pwd:{ticket}，TTL 默认 300 秒。
     * Value 字段：userId、sourceSystemCode、returnUrl、issuedAt、expireAt、nonce。
     * </p>
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return action_ticket
     */
    public String createPwdTicket(long userId, String returnUrl) {
        return createTicket(ACTION_TICKET_PASSWORD_PREFIX, userId, returnUrl);
    }

    /**
     * 生成个人信息维护 action_ticket。
     * <p>
     * Redis Key: portal:action:ticket:profile:{ticket}，TTL 默认 300 秒。
     * Value 字段：userId、sourceSystemCode、returnUrl、issuedAt、expireAt、nonce。
     * </p>
     *
     * @param userId    用户 ID
     * @param returnUrl 回跳地址
     * @return action_ticket
     */
    public String createProfileTicket(long userId, String returnUrl) {
        return createTicket(ACTION_TICKET_PROFILE_PREFIX, userId, returnUrl);
    }

    /**
     * 创建票据并写入 Redis。
     */
    private String createTicket(String prefix, long userId, String returnUrl) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId不能为空");
        }
        validateReturnUrl(returnUrl);
        String ticket = UUID.randomUUID().toString().replace("-", "");
        String key = prefix + ticket;
        long issuedAt = Instant.now().getEpochSecond();
        long expireAt = issuedAt + ttlSeconds;
        ActionTicketPayload payload = new ActionTicketPayload(userId, sourceSystemCode, returnUrl, issuedAt, expireAt, generateNonce());
        String value = toJson(payload);
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        log.info("创建 action_ticket, keyPrefix={}, userId={}, systemCode={}, ttlSeconds={}, returnUrl={}",
                prefix, userId, sourceSystemCode, ttlSeconds, maskReturnUrl(returnUrl));
        return ticket;
    }

    /**
     * 校验回调地址白名单。
     */
    private void validateReturnUrl(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) {
            throw new InvalidReturnUrlException("returnUrl不能为空");
        }
        URI uri;
        try {
            uri = URI.create(returnUrl);
        } catch (IllegalArgumentException ex) {
            throw new InvalidReturnUrlException("returnUrl格式不合法");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new InvalidReturnUrlException("returnUrl仅支持http/https");
        }
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new InvalidReturnUrlException("returnUrl缺少host");
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        boolean allowed = allowedHosts.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
        if (!allowed) {
            throw new InvalidReturnUrlException("returnUrl host不在白名单");
        }
    }

    /**
     * 生成跳转地址。
     */
    private String buildJumpUrl(String path, String ticket) {
        String base = trimTrailingSlash(portalBaseUrl);
        String normalizedPath = ensureLeadingSlash(path);
        return base + normalizedPath + "?ticket=" + ticket;
    }

    /**
     * 序列化对象为 JSON。
     */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生成 action_ticket 失败", ex);
        }
    }

    /**
     * 生成随机 nonce。
     */
    private String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 去除末尾斜杠。
     */
    private String trimTrailingSlash(String baseUrl) {
        String value = baseUrl.trim();
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 确保 path 以 / 开头。
     */
    private String ensureLeadingSlash(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * returnUrl 脱敏日志（host + path 前 30 个字符）。
     */
    private String maskReturnUrl(String returnUrl) {
        try {
            URI uri = URI.create(returnUrl);
            String host = uri.getHost();
            String path = uri.getRawPath();
            if (!StringUtils.hasText(host)) {
                return "unknown";
            }
            String safePath = path == null ? "" : path;
            if (safePath.length() > 30) {
                safePath = safePath.substring(0, 30);
            }
            return host + safePath;
        } catch (IllegalArgumentException ex) {
            return "unknown";
        }
    }

    /**
     * ActionTicket payload，用于序列化。
     */
    public static class ActionTicketPayload {
        private final long userId;
        private final String sourceSystemCode;
        private final String returnUrl;
        private final long issuedAt;
        private final long expireAt;
        private final String nonce;

        public ActionTicketPayload(long userId,
                                   String sourceSystemCode,
                                   String returnUrl,
                                   long issuedAt,
                                   long expireAt,
                                   String nonce) {
            this.userId = userId;
            this.sourceSystemCode = sourceSystemCode;
            this.returnUrl = returnUrl;
            this.issuedAt = issuedAt;
            this.expireAt = expireAt;
            this.nonce = nonce;
        }

        public long getUserId() {
            return userId;
        }

        public String getSourceSystemCode() {
            return sourceSystemCode;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public String getNonce() {
            return nonce;
        }
    }
}
