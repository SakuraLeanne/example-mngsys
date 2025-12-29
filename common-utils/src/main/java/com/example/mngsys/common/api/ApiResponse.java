package com.example.mngsys.common.api;

import java.io.Serializable;

/**
 * ApiResponse。
 * <p>
 * 通用的 API 响应包装类，用于统一接口返回结构。提供成功、失败的静态工厂方法，
 * 并通过泛型携带业务数据，便于客户端解析与状态判断。
 * </p>
 *
 * @param <T> 业务数据载体的类型
 */
public final class ApiResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码，约定 0 为成功，非 0 表示具体业务错误。
     */
    private final int code;

    /**
     * 人类可读的提示信息，便于前端直接展示或记录。
     */
    private final String message;

    /**
     * 业务数据载体，成功时通常包含实体或分页结果，失败时为 {@code null}。
     */
    private final T data;

    /**
     * 私有构造函数，强制通过静态工厂方法创建响应对象，确保状态码语义一致。
     *
     * @param code    业务状态码
     * @param message 提示信息
     * @param data    业务数据载体
     */
    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构建默认成功响应，状态码为 0，提示信息为 "OK"。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 已包装的成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "OK", data);
    }

    /**
     * 构建自定义提示信息的成功响应。
     *
     * @param message 自定义提示信息
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 已包装的成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    /**
     * 根据错误码构建失败响应，使用错误码默认的提示信息。
     *
     * @param errorCode 业务错误码枚举
     * @param <T>       业务数据类型
     * @return 已包装的失败响应
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 根据错误码构建失败响应，允许覆盖默认提示信息。
     *
     * @param errorCode 业务错误码枚举
     * @param message   自定义提示信息
     * @param <T>       业务数据类型
     * @return 已包装的失败响应
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    /**
     * 获取业务状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取提示信息。
     *
     * @return 提示信息文本
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取业务数据。
     *
     * @return 泛型业务数据
     */
    public T getData() {
        return data;
    }
}
