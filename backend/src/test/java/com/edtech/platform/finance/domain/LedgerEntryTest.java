package com.edtech.platform.finance.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryTest {
    @Test
    void appendOnlyEntryRequiresPositiveAmount() {
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), LedgerEntryType.PACKAGE_FUNDED,
                100, BalanceBucket.PENDING, LedgerDirection.CREDIT, "INVOICE", UUID.randomUUID(),
                "payment:1", "funded");
        assertThat(entry.getAmountVnd()).isEqualTo(100);
        assertThatThrownBy(() -> new LedgerEntry(UUID.randomUUID(), LedgerEntryType.ADJUSTMENT,
                0, BalanceBucket.AVAILABLE, LedgerDirection.CREDIT, "ADMIN", UUID.randomUUID(),
                "adjustment:1", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
