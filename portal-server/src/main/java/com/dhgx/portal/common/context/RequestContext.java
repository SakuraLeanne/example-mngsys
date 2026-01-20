package com.dhgx.portal.common.context;

/**
 * RequestContext。
 */
public final class RequestContext {
    /** 使用字符串形式的用户ID，兼容雪花/UUID。 */
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
