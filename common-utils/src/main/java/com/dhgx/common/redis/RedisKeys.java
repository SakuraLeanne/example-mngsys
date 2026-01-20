package com.dhgx.common.redis;

import java.time.Duration;

/**
 * RedisKeys。
 * <p>
 * 统一管理 Redis Key 的命名规范与 TTL，避免硬编码与前后端不一致，
 * 提升可维护性与复用性。
 * </p>
 */
public final class RedisKeys {
    private RedisKeys() {
    }

    /** SSO 临时票据前缀。 */
    public static final String SSO_TICKET_PREFIX = "sso:ticket:";
    /** SSO 临时票据过期时间。 */
    public static final Duration SSO_TICKET_TTL = Duration.ofSeconds(60);

    /** 密码修改场景下的 action_ticket 前缀。 */
    public static final String ACTION_TICKET_PASSWORD_PREFIX = "portal:action:ticket:pwd:";
    /** 个人档案更新场景下的 action_ticket 前缀。 */
    public static final String ACTION_TICKET_PROFILE_PREFIX = "portal:action:ticket:profile:";
    /** action_ticket 统一过期时间。 */
    public static final Duration ACTION_TICKET_TTL = Duration.ofMinutes(5);

    /** 门户 Token 前缀。 */
    public static final String PTK_PREFIX = "portal:ptk:";
    /** 门户 Token 过期时间。 */
    public static final Duration PTK_TTL = Duration.ofMinutes(10);

    /** 用户权限缓存前缀。 */
    public static final String USER_AUTH_PREFIX = "user:auth:";
    /** 用户权限缓存过期时间。 */
    public static final Duration USER_AUTH_TTL = Duration.ofMinutes(10);

    /** 用户 Token 版本号前缀。 */
    public static final String TOKEN_VERSION_PREFIX = "auth:token:version:";

    /** 门户事件流 Key。 */
    public static final String PORTAL_EVENTS_STREAM = "portal:events";

    /**
     * 拼接 SSO Ticket 的完整 Key。
     *
     * @param ticket Ticket 字符串
     * @return Redis Key
     */
    public static String ssoTicket(String ticket) {
        return SSO_TICKET_PREFIX + ticket;
    }

    /**
     * 拼接密码修改场景的 action_ticket Key。
     *
     * @param ticket Ticket 字符串
     * @return Redis Key
     */
    public static String actionPasswordTicket(String ticket) {
        return ACTION_TICKET_PASSWORD_PREFIX + ticket;
    }

    /**
     * 拼接档案更新场景的 action_ticket Key。
     *
     * @param ticket Ticket 字符串
     * @return Redis Key
     */
    public static String actionProfileTicket(String ticket) {
        return ACTION_TICKET_PROFILE_PREFIX + ticket;
    }

    /**
     * 拼接门户 Token Key。
     *
     * @param ptk 门户 Token
     * @return Redis Key
     */
    public static String portalToken(String ptk) {
        return PTK_PREFIX + ptk;
    }

    /**
     * 拼接用户权限缓存 Key。
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String userAuth(long userId) {
        return USER_AUTH_PREFIX + userId;
    }

    /**
     * 拼接用户 Token 版本号 Key。
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String tokenVersion(String userId) {
        return TOKEN_VERSION_PREFIX + userId;
    }

    /**
     * 拼接事件去重 Key（按业务系统区分）。
     *
     * @param systemCode 业务系统标识
     * @param eventId    事件唯一标识
     * @return Redis Key
     */
    public static String eventDedup(String systemCode, String eventId) {
        return String.format("event:dedup:%s:%s", systemCode, eventId);
    }
}
