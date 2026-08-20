package com.edtech.platform.common.exception;

public class DomainConcurrentModificationException extends BusinessException {
    public DomainConcurrentModificationException(Object... args) {
        super(ErrorCode.CONCURRENT_MODIFICATION, args);
    }
}
