package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePayoutRequest(
        @NotNull UUID bankAccountId,
        @Min(1000) long amountVnd,
        String teacherNote,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Long walletVersion
) {}
