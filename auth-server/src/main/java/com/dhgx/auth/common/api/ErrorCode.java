package com.dhgx.auth.common.api;

/**
 * ErrorCode。
 */
public enum ErrorCode {
    UNAUTHENTICATED(100100, 401, "登录已失效，请先登录"),
    FORBIDDEN(100200, 403, "暂无访问权限，请联系管理员"),
    INVALID_ARGUMENT(100300, 400, "请求参数有误，请检查后重试"),
    NOT_FOUND(100404, 404, "资源不存在或已被删除"),
    INTERNAL_ERROR(100500, 500, "系统开小差了，请稍后再试");

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
