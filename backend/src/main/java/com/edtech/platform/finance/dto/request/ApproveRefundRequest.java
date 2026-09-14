package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ApproveRefundRequest(@Min(1) int approvedSessions, String adminNote,
                                   @NotNull @Min(0) Long version) {}
