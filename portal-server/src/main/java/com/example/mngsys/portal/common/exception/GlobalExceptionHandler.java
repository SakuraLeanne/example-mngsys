package com.example.mngsys.portal.common.exception;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, ex.getMessage()));
    }
}
