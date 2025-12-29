package com.example.mngsys.common.event;

/**
 * PortalEventType。
 * <p>
 * 门户事件类型的枚举定义，描述用户相关的事件分类，用于事件流或审计日志。
 * </p>
 */
public enum PortalEventType {
    /** 用户密码已被修改。 */
    USER_PASSWORD_CHANGED,
    /** 用户基础信息已更新。 */
    USER_PROFILE_UPDATED,
    /** 用户被禁用。 */
    USER_DISABLED,
    /** 用户已启用/解禁。 */
    USER_ENABLED
}
