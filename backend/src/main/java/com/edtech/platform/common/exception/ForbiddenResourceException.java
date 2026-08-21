package com.edtech.platform.common.exception;

public class ForbiddenResourceException extends BusinessException {
    public ForbiddenResourceException(Object... args) {
        super(ErrorCode.FORBIDDEN_RESOURCE, args);
    }
}
