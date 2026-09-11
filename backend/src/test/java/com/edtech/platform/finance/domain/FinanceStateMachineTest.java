package com.edtech.platform.finance.domain;

import com.edtech.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceStateMachineTest {
    private static final Instant NOW = Instant.parse("2026-09-11T00:00:00Z");
    private static final UUID ADMIN = UUID.randomUUID();

    @Test
    void refundCannotCompleteBeforeApprovalOrRejectAfterApproval() {
        RefundRequest refund = RefundRequest.create(UUID.randomUUID(), UUID.randomUUID(), "reason", 1,
                "VCB", "970436", "enc", "NGUYEN VAN A");

        assertThatThrownBy(() -> refund.complete(ADMIN, "ref", NOW, "proof", "url", NOW))
                .isInstanceOf(BusinessException.class);

        refund.approve(ADMIN, 1, 100_000L, "approved", NOW);
        assertThatThrownBy(() -> refund.reject(ADMIN, "rejected", NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void payoutCannotCompleteBeforeProcessing() {
        PayoutRequest payout = PayoutRequest.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                100_000L, "note");

        assertThatThrownBy(() -> payout.complete(ADMIN, "ref", NOW, "proof", "url", NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void terminalStatesCannotTransition() {
        PayoutRequest payout = PayoutRequest.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                100_000L, "note");
        payout.process(ADMIN, NOW);
        payout.complete(ADMIN, "ref", NOW, "proof", "url", NOW);

        assertThatThrownBy(() -> payout.reject(ADMIN, "retry", NOW))
                .isInstanceOf(BusinessException.class);

        RefundRequest refund = RefundRequest.create(UUID.randomUUID(), UUID.randomUUID(), "reason", 1,
                "VCB", "970436", "enc", "NGUYEN VAN A");
        refund.approve(ADMIN, 1, 100_000L, "approved", NOW);
        refund.complete(ADMIN, "ref", NOW, "proof", "url", NOW);

        assertThatThrownBy(() -> refund.complete(ADMIN, "ref2", NOW, "proof", "url", NOW))
                .isInstanceOf(BusinessException.class);
    }
}
