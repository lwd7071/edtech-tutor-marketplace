package com.edtech.platform.common.response;

import java.util.List;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    List<ApiErrorDetail> errors,
    PageMeta meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return ok(null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return created("Tạo dữ liệu thành công", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    public static <T> ApiResponse<T> page(T data, PageMeta meta) {
        return page(null, data, meta);
    }

    public static <T> ApiResponse<T> page(String message, T data, PageMeta meta) {
        return new ApiResponse<>(true, message, data, null, meta);
    }

    public static <T> ApiResponse<T> error(String message, List<ApiErrorDetail> errors) {
        return new ApiResponse<>(false, message, null, errors, null);
    }
}
