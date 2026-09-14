package com.edtech.platform.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(args != null && args.length > 0 && args[0] instanceof String text && !text.isBlank()
                ? text : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = args;
    }
}
