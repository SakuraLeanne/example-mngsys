package com.example.mngsys.auth.common.exception;

import com.example.mngsys.auth.common.api.ApiResponse;
import com.example.mngsys.auth.common.api.ErrorCode;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * GlobalExceptionHandler。
 * <p>
 * 统一拦截控制层异常，转换为标准化的业务响应。
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理未登录异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.UNAUTHENTICATED, ex.getMessage()));
    }

    /**
     * 处理无权限异常。
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN, ex.getMessage()));
    }

    /**
     * 处理参数绑定校验异常。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        FieldError fieldError = null;
        if (ex instanceof MethodArgumentNotValidException) {
            fieldError = ((MethodArgumentNotValidException) ex).getBindingResult().getFieldError();
        } else if (ex instanceof BindException) {
            fieldError = ((BindException) ex).getBindingResult().getFieldError();
        }
        String message = fieldError == null ? ErrorCode.INVALID_ARGUMENT.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    /**
     * 处理约束校验异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, ex.getMessage()));
    }

    /**
     * 处理非法参数异常。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, ex.getMessage()));
    }

    /**
     * 兜底处理未知异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, ex.getMessage()));
    }
}
