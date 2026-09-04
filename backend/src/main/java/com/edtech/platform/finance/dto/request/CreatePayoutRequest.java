package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePayoutRequest(
        @NotNull UUID bankAccountId,
        @Min(1000) long amountVnd,
        String teacherNote,
        long walletVersion
) {}