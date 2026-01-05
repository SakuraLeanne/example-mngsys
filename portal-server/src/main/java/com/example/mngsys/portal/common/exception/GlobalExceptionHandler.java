package com.example.mngsys.portal.common.exception;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
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
        String message = fieldError == null ? ErrorCode.INVALID_ARGUMENT.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(resolveMessage("error.request.invalid", ErrorCode.INVALID_ARGUMENT.getMessage()));
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT,
                        resolveMessage("error.request.invalid", ErrorCode.INVALID_ARGUMENT.getMessage())));
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
}
