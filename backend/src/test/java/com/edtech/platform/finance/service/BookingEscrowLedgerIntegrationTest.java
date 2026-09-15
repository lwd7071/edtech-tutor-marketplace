package com.edtech.platform.finance.service;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.finance.facade.FinanceFacade;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingEscrowLedgerIntegrationTest extends AbstractIntegrationTest {
    private static final UUID TEACHER = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING = UUID.fromString("60000000-0000-0000-0000-000000000002");

    @Autowired FinanceFacade finance;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    @Transactional
    void holdAndReleaseKeepWalletAndBothLedgersBalanced() {
        long pendingBefore = wallet("pending_balance_vnd");
        long availableBefore = wallet("available_balance_vnd");
        long heldBefore = escrow();

        finance.holdBookingSession(TEACHER, BOOKING, 100);
        entityManager.flush();
        assertThat(wallet("pending_balance_vnd")).isEqualTo(pendingBefore - 100);
        assertThat(teacherLedger("PENDING")).isEqualTo(-100);
        assertThat(escrow()).isEqualTo(heldBefore + 100);

        finance.releaseHeldBookingSession(TEACHER, BOOKING, 100);
        entityManager.flush();
        assertThat(wallet("available_balance_vnd")).isEqualTo(availableBefore + 100);
        assertThat(teacherLedger("AVAILABLE")).isEqualTo(100);
        assertThat(escrow()).isEqualTo(heldBefore);
        assertThat(revenue()).isZero();
    }

    @Test
    @Transactional
    void retainMovesEscrowToRevenueWithoutCreditingTeacher() {
        long pendingBefore = wallet("pending_balance_vnd");
        long availableBefore = wallet("available_balance_vnd");

        finance.holdBookingSession(TEACHER, BOOKING, 100);
        finance.retainHeldBookingSession(BOOKING, 100);
        entityManager.flush();

        assertThat(wallet("pending_balance_vnd")).isEqualTo(pendingBefore - 100);
        assertThat(wallet("available_balance_vnd")).isEqualTo(availableBefore);
        assertThat(teacherLedger("PENDING")).isEqualTo(-100);
        assertThat(teacherLedger("AVAILABLE")).isZero();
        assertThat(escrow()).isZero();
        assertThat(revenue()).isEqualTo(100);
    }

    private long wallet(String column) {
        return jdbc.queryForObject("select " + column + " from wallets where teacher_id = ?", Long.class, TEACHER);
    }

    private long teacherLedger(String bucket) {
        return jdbc.queryForObject("""
                select coalesce(sum(case when direction='CREDIT' then amount_vnd else -amount_vnd end),0)
                from ledger_entries where reference_type='BOOKING' and reference_id=? and balance_bucket=?
                """, Long.class, BOOKING, bucket);
    }

    private long escrow() { return platformBucket("ESCROW"); }
    private long revenue() { return platformBucket("REVENUE"); }

    private long platformBucket(String bucket) {
        return jdbc.queryForObject("""
                select coalesce(sum(case when direction='CREDIT' then amount_vnd else -amount_vnd end),0)
                from platform_ledger_entries where booking_id=? and bucket=?
                """, Long.class, BOOKING, bucket);
    }
}
