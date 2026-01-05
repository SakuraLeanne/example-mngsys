package com.example.mngsys.portal.common.exception;

import com.example.mngsys.portal.common.api.ErrorCode;

/**
 * LocalizedBusinessException.
 * <p>
 * 业务异常，携带用于国际化的消息键与占位符参数。
 * </p>
 */
public class LocalizedBusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String messageKey;
    private final Object[] args;
    private final String defaultMessage;

    public LocalizedBusinessException(ErrorCode errorCode, String messageKey, String defaultMessage, Object... args) {
        super(defaultMessage);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.args = args;
        this.defaultMessage = defaultMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
