package com.dhgx.auth.common.exception;

import com.dhgx.auth.common.api.ApiResponse;
import com.dhgx.auth.common.api.ErrorCode;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 处理未登录异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.UNAUTHENTICATED, resolveNotLoginMessage(ex)));
    }

    /**
     * 处理无权限异常。
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage()));
    }

    /**
     * 处理业务异常，支持国际化提示。
     */
    @ExceptionHandler(LocalizedBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(LocalizedBusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode() == null ? ErrorCode.INVALID_ARGUMENT : ex.getErrorCode();
        String message = resolveMessage(ex.getMessageKey(), ex.getDefaultMessage(), ex.getArgs());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, message));
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
        String message = fieldError == null
                ? ErrorCode.INVALID_ARGUMENT.getMessage()
                : buildFieldMessage(fieldError.getField(), fieldError.getDefaultMessage());
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    /**
     * 处理约束校验异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> buildFieldMessage(normalizeFieldName(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .orElse(resolveMessage("error.request.invalid", ErrorCode.INVALID_ARGUMENT.getMessage()));
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    /**
     * 处理非法参数异常。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = StringUtils.hasText(ex.getMessage())
                ? ex.getMessage()
                : resolveMessage("error.request.invalid", ErrorCode.INVALID_ARGUMENT.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    /**
     * 兜底处理未知异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception caught by global handler", ex);
        String message = resolveMessage("error.server", ErrorCode.INTERNAL_ERROR.getMessage());
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, message));
    }

    private String resolveMessage(String messageKey, String defaultMessage, Object... args) {
        if (messageKey == null) {
            return defaultMessage;
        }
        return messageSource.getMessage(messageKey, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    private String resolveNotLoginMessage(NotLoginException ex) {
        String type = ex.getType();
        if (NotLoginException.NOT_TOKEN.equals(type)) {
            return "登录凭证缺失，请先登录";
        }
        if (NotLoginException.INVALID_TOKEN.equals(type)) {
            return "登录凭证无效，请重新登录";
        }
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            return "登录凭证已过期，请重新登录";
        }
        if (NotLoginException.BE_REPLACED.equals(type)) {
            return "账号已在其他设备登录，请重新登录";
        }
        if (NotLoginException.KICK_OUT.equals(type)) {
            return "账号已被下线，请重新登录";
        }
        if (NotLoginException.TOKEN_FREEZE.equals(type)) {
            return "账号已被冻结，请联系管理员";
        }
        return ErrorCode.UNAUTHENTICATED.getMessage();
    }

    private String buildFieldMessage(String field, String detail) {
        String fallback = resolveMessage("error.request.invalid", ErrorCode.INVALID_ARGUMENT.getMessage());
        String resolvedDetail = StringUtils.hasText(detail) ? detail : fallback;
        if (!StringUtils.hasText(field)) {
            return resolvedDetail;
        }
        if (resolvedDetail.contains(field)) {
            return resolvedDetail;
        }
        return "参数" + field + resolvedDetail;
    }

    private String normalizeFieldName(String fieldPath) {
        if (!StringUtils.hasText(fieldPath)) {
            return fieldPath;
        }
        int lastDot = fieldPath.lastIndexOf('.');
        return lastDot >= 0 ? fieldPath.substring(lastDot + 1) : fieldPath;
    }
}
