package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.BalanceBucket;
import com.edtech.platform.finance.domain.LedgerDirection;
import com.edtech.platform.finance.domain.LedgerEntry;
import com.edtech.platform.finance.domain.LedgerEntryType;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryView(
        UUID id,
        UUID walletId,
        LedgerEntryType entryType,
        long amountVnd,
        BalanceBucket balanceBucket,
        LedgerDirection direction,
        String referenceType,
        UUID referenceId,
        String description,
        Instant createdAt
) {
    public static LedgerEntryView from(LedgerEntry entry) {
        return new LedgerEntryView(
                entry.getId(),
                entry.getWalletId(),
                entry.getEntryType(),
                entry.getAmountVnd(),
                entry.getBalanceBucket(),
                entry.getDirection(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}
