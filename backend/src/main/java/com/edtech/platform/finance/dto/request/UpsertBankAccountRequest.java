package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertBankAccountRequest(
        @NotBlank String bankBin,
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String accountHolderName,
        Boolean isDefault,
        @NotNull @Min(0) Long version
 ) {
    public UpsertBankAccountRequest(String bankBin, String bankName, String accountNumber, String accountHolderName, Boolean isDefault) {
        this(bankBin, bankName, accountNumber, accountHolderName, isDefault, 0L);
    }
 }
