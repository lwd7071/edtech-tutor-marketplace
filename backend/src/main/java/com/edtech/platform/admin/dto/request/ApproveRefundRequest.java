package com.edtech.platform.admin.dto.request;

import jakarta.validation.constraints.Min;

public record ApproveRefundRequest(
        @Min(1) int approvedSessions,
        String adminNote,
        long version
) {}