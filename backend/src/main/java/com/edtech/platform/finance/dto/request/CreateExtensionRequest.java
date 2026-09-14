package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateExtensionRequest(
        @NotNull UUID studentPackageId,
        @NotBlank String reason,
        @NotNull Instant requestedExpiryDate,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Long packageVersion
) {
    public CreateExtensionRequest(UUID studentPackageId, String reason, Instant requestedExpiryDate) {
        this(studentPackageId, reason, requestedExpiryDate, 0L);
    }
}
