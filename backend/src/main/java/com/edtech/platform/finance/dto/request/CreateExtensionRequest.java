package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateExtensionRequest(
        @NotNull UUID studentPackageId,
        @NotBlank String reason,
        @NotNull Instant requestedExpiryDate
) {}