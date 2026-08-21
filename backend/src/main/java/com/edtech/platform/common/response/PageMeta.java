package com.edtech.platform.common.response;

public record PageMeta(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {}
