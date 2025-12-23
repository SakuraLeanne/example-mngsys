package com.example.mngsys.auth.common.api;

public enum ErrorCode {
    UNAUTHENTICATED(100100, 401, "未登录"),
    FORBIDDEN(100200, 403, "无权限"),
    INVALID_ARGUMENT(100300, 400, "参数错误"),
    NOT_FOUND(100404, 404, "资源不存在"),
    INTERNAL_ERROR(100500, 500, "系统错误");

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
