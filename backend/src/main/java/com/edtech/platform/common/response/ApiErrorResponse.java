package com.edtech.platform.common.response;

import java.time.Instant;

public record ApiErrorResponse(
    boolean success,
    ApiError error,
    Instant timestamp,
    String requestId
) {}
