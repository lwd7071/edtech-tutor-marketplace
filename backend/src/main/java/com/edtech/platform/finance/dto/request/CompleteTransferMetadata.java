package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/** Non-file metadata for a payout/refund completion multipart request. */
public record CompleteTransferMetadata(
        @NotBlank @Size(max = 255) String bankReference,
        @NotNull Instant transferredAt,
        @NotNull @Min(0) Long version
) {}
