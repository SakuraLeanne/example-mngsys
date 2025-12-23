package com.example.mngsys.common.redis;

import java.time.Duration;

/**
 * RedisKeys。
 */
public final class RedisKeys {
    private RedisKeys() {
    }

    public static final String SSO_TICKET_PREFIX = "sso:ticket:";
    public static final Duration SSO_TICKET_TTL = Duration.ofSeconds(60);

    public static final String ACTION_TICKET_PASSWORD_PREFIX = "portal:action:ticket:pwd:";
    public static final String ACTION_TICKET_PROFILE_PREFIX = "portal:action:ticket:profile:";
    public static final Duration ACTION_TICKET_TTL = Duration.ofMinutes(5);

    public static final String PTK_PREFIX = "portal:ptk:";
    public static final Duration PTK_TTL = Duration.ofMinutes(10);

    public static final String USER_AUTH_PREFIX = "user:auth:";
    public static final Duration USER_AUTH_TTL = Duration.ofMinutes(10);

    public static final String PORTAL_EVENTS_STREAM = "portal:events";

    public static String ssoTicket(String ticket) {
        return SSO_TICKET_PREFIX + ticket;
    }

    public static String actionPasswordTicket(String ticket) {
        return ACTION_TICKET_PASSWORD_PREFIX + ticket;
    }

    public static String actionProfileTicket(String ticket) {
        return ACTION_TICKET_PROFILE_PREFIX + ticket;
    }

    public static String portalToken(String ptk) {
        return PTK_PREFIX + ptk;
    }

    public static String userAuth(long userId) {
        return USER_AUTH_PREFIX + userId;
    }

    public static String eventDedup(String systemCode, String eventId) {
        return String.format("event:dedup:%s:%s", systemCode, eventId);
    }
}
