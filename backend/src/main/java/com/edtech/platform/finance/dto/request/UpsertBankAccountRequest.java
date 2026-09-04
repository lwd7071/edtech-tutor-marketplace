package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpsertBankAccountRequest(
        @NotBlank String bankBin,
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String accountHolderName,
        Boolean isDefault
) {}