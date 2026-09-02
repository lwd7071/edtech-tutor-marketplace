package com.edtech.platform.finance.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {
    @Test
    void balancePrimitivesPreserveNonNegativeBuckets() {
        Wallet wallet = Wallet.forTeacher(UUID.randomUUID());
        wallet.creditPending(100);
        wallet.debitPending(40);
        wallet.creditAvailable(80);
        wallet.reserveAvailable(30);
        wallet.releaseReserved(10);
        assertThat(wallet.getPendingBalanceVnd()).isEqualTo(60);
        assertThat(wallet.getAvailableBalanceVnd()).isEqualTo(60);
        assertThat(wallet.getReservedBalanceVnd()).isEqualTo(20);
        assertThatThrownBy(() -> wallet.debitPending(61)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsOverflowAndNonPositiveMutation() {
        Wallet wallet = Wallet.forTeacher(UUID.randomUUID());
        wallet.creditAvailable(Long.MAX_VALUE);
        assertThatThrownBy(() -> wallet.creditAvailable(1)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> wallet.creditPending(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
