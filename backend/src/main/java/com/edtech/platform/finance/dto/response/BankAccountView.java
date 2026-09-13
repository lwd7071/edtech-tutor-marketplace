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
        Instant createdAt,
        long version
) {
    public BankAccountView(UUID id, String bankBin, String bankName, String accountNumberMasked,
                           String accountHolderName, boolean isVerified, boolean isDefault, Instant createdAt) {
        this(id, bankBin, bankName, accountNumberMasked, accountHolderName, isVerified, isDefault, createdAt, 0L);
    }
}
