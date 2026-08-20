package com.edtech.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record ApiResponse<T>(
    boolean success,
    T data,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    PageMeta meta,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> page(T data, PageMeta meta) {
        return new ApiResponse<>(true, data, meta, Instant.now());
    }
}
