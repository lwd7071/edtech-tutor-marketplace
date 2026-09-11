package com.edtech.platform.enrollment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentPackageTest {
    @Test
    void paidFactoryCreatesActivePackageWithExactSnapshotsAndCounters() {
        Instant paidAt = Instant.parse("2026-08-27T00:00:00Z");
        StudentPackage value = StudentPackage.activateAfterPayment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Math 1-1", 10, 30, 1_000_000L, new BigDecimal("5.00"), paidAt);

        assertThat(value.getStatus()).isEqualTo(StudentPackageStatus.ACTIVE);
        assertThat(value.getRemainingSessions()).isEqualTo(10);
        assertThat(value.getReservedSessions()).isZero();
        assertThat(value.getCompletedSessions()).isZero();
        assertThat(value.getRefundedSessions()).isZero();
        assertThat(value.getStartsAt()).isEqualTo(paidAt);
        assertThat(value.getExpiresAt()).isEqualTo(paidAt.plusSeconds(30L * 86_400));
        value.validateCounterTotal();
    }

    @Test
    void factoryCannotCreateLegacyPendingPaymentAndRejectsInvalidInvariants() {
        assertThat(StudentPackage.class.getConstructors()).isEmpty();
        assertThatThrownBy(() -> StudentPackage.activateAfterPayment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Bad", 0, 30, 1, new BigDecimal("5.00"), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(StudentPackageStatus.PENDING_PAYMENT).isNotEqualTo(StudentPackageStatus.ACTIVE);
    }

    @Test
    void refundAndExtensionRequireTheirSourceStates() {
        Instant now = Instant.parse("2026-09-11T00:00:00Z");
        StudentPackage active = StudentPackage.activateAfterPayment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Math", 3, 30, 300_000L, new BigDecimal("5.00"), now);

        assertThatThrownBy(() -> active.applyRefund(1, now))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> active.extendExpiry(now.plusSeconds(86_400), now))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> active.restoreFromRefundPending(now))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(active::releaseReservedSession)
                .isInstanceOf(IllegalStateException.class);
    }
}
