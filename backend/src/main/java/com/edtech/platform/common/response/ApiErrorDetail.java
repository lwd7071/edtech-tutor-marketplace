package com.edtech.platform.common.response;

public record ApiErrorDetail(
        String code,
        String field,
        String message
) {
}
