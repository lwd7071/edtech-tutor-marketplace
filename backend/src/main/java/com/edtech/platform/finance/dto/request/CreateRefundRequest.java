package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRefundRequest(
        @NotNull UUID studentPackageId,
        @NotBlank String reason,
        @Min(1) int requestedSessions,
        String bankName,
        String bankBin,
        String accountNumber,
        String accountHolderName
) {}