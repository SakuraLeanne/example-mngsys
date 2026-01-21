package com.dhgx.portal.common.api;

/**
 * ErrorCode。
 */
public enum ErrorCode {
    UNAUTHENTICATED(100100, 401, "登录已失效，请先登录"),
    FORBIDDEN(100200, 403, "暂无访问权限，请联系管理员"),
    INVALID_ARGUMENT(100300, 400, "请求参数有误，请检查后重试"),
    INVALID_RETURN_URL(100301, 400, "回调地址不合法或不在白名单"),
    CAPTCHA_REQUIRED(100310, 400, "请先完成验证码校验"),
    CAPTCHA_INVALID(100311, 400, "验证码无效，请重新获取"),
    AUTH_FAILED(100320, 401, "账号或密码错误"),
    NOT_FOUND(100404, 404, "资源不存在或已被删除"),
    INTERNAL_ERROR(100500, 500, "系统开小差了，请稍后再试"),

    ACTION_TICKET_INVALID(200110, 410, "校验票据无效，请重新发起操作"),
    ACTION_TICKET_EXPIRED(200111, 410, "校验票据已过期，请重新发起操作"),
    ACTION_TICKET_REPLAYED(200112, 409, "校验票据已被使用，请重新发起操作"),

    PTK_INVALID(200120, 401, "登录凭证无效，请重新登录"),
    PTK_EXPIRED(200121, 410, "登录凭证已过期，请重新登录"),
    PTK_SCOPE_MISMATCH(200122, 403, "登录凭证权限不匹配，请确认访问范围"),

    USER_DISABLED(300100, 403, "账号已被停用，请联系管理员"),
    OLD_PASSWORD_INCORRECT(300110, 400, "旧密码不正确，请重试"),
    NEW_PASSWORD_POLICY_VIOLATION(300111, 400, "新密码不符合安全策略，请修改后重试"),

    SSO_TICKET_INVALID(400210, 400, "票据无效或已过期，请重新发起登录"),
    SSO_TICKET_CLIENT_MISMATCH(400211, 403, "业务系统不匹配，请确认 systemCode 是否正确"),
    SSO_TICKET_REDIRECT_URI_MISMATCH(400212, 403, "回跳地址不匹配，请检查 redirectUri"),
    SSO_TICKET_STATE_MISMATCH(400213, 409, "状态校验失败，请重新发起登录"),
    SSO_TICKET_RATE_LIMITED(400214, 429, "请求过于频繁，请稍后再试"),
    SSO_TICKET_SYSTEM_ERROR(400215, 500, "系统异常，请稍后再试");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
