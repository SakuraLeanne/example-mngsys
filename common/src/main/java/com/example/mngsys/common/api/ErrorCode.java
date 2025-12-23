package com.example.mngsys.common.api;

public enum ErrorCode {
    UNAUTHENTICATED(100100, 401, "未登录"),
    FORBIDDEN(100200, 403, "无权限"),
    INVALID_ARGUMENT(100300, 400, "参数错误"),
    INVALID_RETURN_URL(100301, 400, "returnUrl非法"),
    NOT_FOUND(100404, 404, "资源不存在"),
    INTERNAL_ERROR(100500, 500, "系统错误"),

    ACTION_TICKET_INVALID(200110, 410, "action_ticket无效"),
    ACTION_TICKET_EXPIRED(200111, 410, "action_ticket过期"),
    ACTION_TICKET_REPLAYED(200112, 409, "action_ticket已使用/重放"),

    PTK_INVALID(200120, 401, "ptk无效"),
    PTK_EXPIRED(200121, 410, "ptk过期"),
    PTK_SCOPE_MISMATCH(200122, 403, "ptk scope不匹配"),

    USER_DISABLED(300100, 403, "用户禁用"),
    OLD_PASSWORD_INCORRECT(300110, 400, "旧密码错误"),
    NEW_PASSWORD_POLICY_VIOLATION(300111, 400, "新密码不符合策略"),

    ROLE_CODE_DUPLICATE(400100, 409, "角色编码重复"),
    MENU_PATH_DUPLICATE(400110, 409, "菜单path重复"),
    MENU_DELETE_CONFLICT(400120, 409, "删除失败存在子菜单/引用");

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
