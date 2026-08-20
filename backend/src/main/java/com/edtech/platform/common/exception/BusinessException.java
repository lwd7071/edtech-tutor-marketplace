package com.edtech.platform.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = args;
    }
}
