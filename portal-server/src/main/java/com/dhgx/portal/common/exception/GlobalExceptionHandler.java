package com.dhgx.portal.common.exception;

import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
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

@RestControllerAdvice
/**
 * GlobalExceptionHandler。
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = StringUtils.hasText(ex.getMessage())
                ? ex.getMessage()
                : resolveMessage("error.request.invalid", ErrorCode.INVALID_ARGUMENT.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    @ExceptionHandler(LocalizedBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(LocalizedBusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode() == null ? ErrorCode.INVALID_ARGUMENT : ex.getErrorCode();
        String message = resolveMessage(ex.getMessageKey(), ex.getDefaultMessage(), ex.getArgs());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, message));
    }

    @ExceptionHandler(InvalidReturnUrlException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidReturnUrl(InvalidReturnUrlException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_RETURN_URL.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_RETURN_URL, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception caught by global handler", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR,
                        resolveMessage("error.server", ErrorCode.INTERNAL_ERROR.getMessage())));
    }

    private String resolveMessage(String messageKey, String defaultMessage, Object... args) {
        if (messageKey == null) {
            return defaultMessage;
        }
        return messageSource.getMessage(messageKey, args, defaultMessage, LocaleContextHolder.getLocale());
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
