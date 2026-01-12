package com.example.mngsys.common.api;

/**
 * ErrorCode。
 * <p>
 * 统一定义网关、门户等业务模块使用的错误码及对应的 HTTP 状态、默认提示信息，
 * 便于前后端协同以及日志排查。数值段按业务域预留，避免冲突。
 * </p>
 */
public enum ErrorCode {
    /** 未登录或会话失效。 */
    UNAUTHENTICATED(100100, 401, "登录已失效，请先登录"),
    /** 登录但无访问权限。 */
    FORBIDDEN(100200, 403, "暂无访问权限，请联系管理员"),
    /** 参数不合法或缺失。 */
    INVALID_ARGUMENT(100300, 400, "请求参数有误，请检查后重试"),
    /** returnUrl 参数非法。 */
    INVALID_RETURN_URL(100301, 400, "回调地址不合法或不在白名单"),
    /** 资源不存在。 */
    NOT_FOUND(100404, 404, "资源不存在或已被删除"),
    /** 未知或服务端内部错误。 */
    INTERNAL_ERROR(100500, 500, "系统开小差了，请稍后再试"),

    /** action_ticket 校验失败。 */
    ACTION_TICKET_INVALID(200110, 410, "校验票据无效，请重新发起操作"),
    /** action_ticket 已过期。 */
    ACTION_TICKET_EXPIRED(200111, 410, "校验票据已过期，请重新发起操作"),
    /** action_ticket 被重放或重复使用。 */
    ACTION_TICKET_REPLAYED(200112, 409, "校验票据已被使用，请重新发起操作"),

    /** 门户令牌无效。 */
    PTK_INVALID(200120, 401, "登录凭证无效，请重新登录"),
    /** 门户令牌过期。 */
    PTK_EXPIRED(200121, 410, "登录凭证已过期，请重新登录"),
    /** 门户令牌 scope 不匹配。 */
    PTK_SCOPE_MISMATCH(200122, 403, "登录凭证权限不匹配，请确认访问范围"),

    /** 用户已被禁用。 */
    USER_DISABLED(300100, 403, "账号已被停用，请联系管理员"),
    /** 旧密码校验失败。 */
    OLD_PASSWORD_INCORRECT(300110, 400, "旧密码不正确，请重试"),
    /** 新密码未满足策略要求。 */
    NEW_PASSWORD_POLICY_VIOLATION(300111, 400, "新密码不符合安全策略，请修改后重试"),

    /** 角色编码重复。 */
    ROLE_CODE_DUPLICATE(400100, 409, "角色编码已存在，请更换后重试"),
    /** 菜单路径重复。 */
    MENU_PATH_DUPLICATE(400110, 409, "菜单路径已存在，请调整后再保存"),
    /** 存在子菜单或引用导致菜单删除冲突。 */
    MENU_DELETE_CONFLICT(400120, 409, "该菜单存在子菜单或被引用，无法删除");

    /** 业务状态码。 */
    private final int code;
    /** 推荐的 HTTP 状态码，便于网关或控制器设置响应。 */
    private final int httpStatus;
    /** 默认的用户可见提示信息。 */
    private final String message;

    /**
     * 枚举构造器。
     *
     * @param code       业务状态码
     * @param httpStatus 推荐 HTTP 状态码
     * @param message    默认提示信息
     */
    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取推荐的 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 获取默认提示信息。
     *
     * @return 默认提示文案
     */
    public String getMessage() {
        return message;
    }
}
