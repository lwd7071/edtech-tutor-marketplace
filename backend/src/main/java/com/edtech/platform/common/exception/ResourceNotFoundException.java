package com.edtech.platform.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(Object... args) {
        super(ErrorCode.RESOURCE_NOT_FOUND, args);
    }
}
