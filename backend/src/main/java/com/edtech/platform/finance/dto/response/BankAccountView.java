package com.edtech.platform.finance.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BankAccountView(
        UUID id,
        String bankBin,
        String bankName,
        String accountNumberMasked,
        String accountHolderName,
        boolean isVerified,
        boolean isDefault,
        Instant createdAt
) {
}
