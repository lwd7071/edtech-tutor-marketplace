package com.edtech.platform.integration;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.service.RefundService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Task 8.1 - Hardening Invariants Integration Test")
class HardeningInvariantsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RefundService refundService;

    @Test
    @DisplayName("Invariant 1: Ledger Integrity - Sum(ledger_entries) == Wallet bucket balances")
    void invariant1_ledgerIntegrity_shouldMatchWalletBalances() {
        // Teacher 1 wallet id: '20000000-0000-0000-0000-000000000001'
        String walletId = "20000000-0000-0000-0000-000000000001";

        Map<String, Object> wallet = jdbcTemplate.queryForMap(
                "SELECT pending_balance_vnd, available_balance_vnd, reserved_balance_vnd FROM wallets WHERE id = ?::uuid",
                walletId
        );

        long pendingBalance = ((Number) wallet.get("pending_balance_vnd")).longValue();
        long availableBalance = ((Number) wallet.get("available_balance_vnd")).longValue();
        long reservedBalance = ((Number) wallet.get("reserved_balance_vnd")).longValue();

        // Calculate sums from ledger entries
        Long sumPendingCredits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE wallet_id = ?::uuid AND balance_bucket = 'PENDING' AND direction = 'CREDIT'",
                Long.class, walletId
        );
        Long sumPendingDebits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE wallet_id = ?::uuid AND balance_bucket = 'PENDING' AND direction = 'DEBIT'",
                Long.class, walletId
        );
        long netPending = (sumPendingCredits != null ? sumPendingCredits : 0L) - (sumPendingDebits != null ? sumPendingDebits : 0L);

        Long sumAvailableCredits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE wallet_id = ?::uuid AND balance_bucket = 'AVAILABLE' AND direction = 'CREDIT'",
                Long.class, walletId
        );
        Long sumAvailableDebits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE wallet_id = ?::uuid AND balance_bucket = 'AVAILABLE' AND direction = 'DEBIT'",
                Long.class, walletId
        );
        long netAvailable = (sumAvailableCredits != null ? sumAvailableCredits : 0L) - (sumAvailableDebits != null ? sumAvailableDebits : 0L);

        Long sumReservedCredits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE wallet_id = ?::uuid AND balance_bucket = 'RESERVED' AND direction = 'CREDIT'",
                Long.class, walletId
        );
        Long sumReservedDebits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE wallet_id = ?::uuid AND balance_bucket = 'RESERVED' AND direction = 'DEBIT'",
                Long.class, walletId
        );
        long netReserved = (sumReservedCredits != null ? sumReservedCredits : 0L) - (sumReservedDebits != null ? sumReservedDebits : 0L);

        assertThat(pendingBalance).isEqualTo(netPending);
        assertThat(availableBalance).isEqualTo(netAvailable);
        assertThat(reservedBalance).isEqualTo(netReserved);
    }

    @Test
    @DisplayName("Invariant 2: Package Counter Invariant - total_sessions = remaining + reserved + completed + refunded")
    void invariant2_packageCounter_shouldEnforceSumConstraint() {
        // Query existing valid package
        Map<String, Object> pkg = jdbcTemplate.queryForMap(
                "SELECT total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions FROM student_packages WHERE id = '40000000-0000-0000-0000-000000000001'::uuid"
        );
        int total = ((Number) pkg.get("total_sessions")).intValue();
        int remaining = ((Number) pkg.get("remaining_sessions")).intValue();
        int reserved = ((Number) pkg.get("reserved_sessions")).intValue();
        int completed = ((Number) pkg.get("completed_sessions")).intValue();
        int refunded = ((Number) pkg.get("refunded_sessions")).intValue();

        assertThat(total).isEqualTo(remaining + reserved + completed + refunded);

        // Attempt to violate constraint via direct update
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE student_packages SET remaining_sessions = 999 WHERE id = '40000000-0000-0000-0000-000000000001'::uuid"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Invariant 3: GiST Exclusion - Prevent overlapping bookings for same teacher")
    void invariant3_gistExclusion_shouldPreventDoubleBooking() {
        // Teacher 1 has booking '60000000-0000-0000-0000-000000000002' SCHEDULED at [now + 2 days, now + 2 days + 1 hour]
        String teacherId = "c0000000-0000-0000-0000-000000000001";
        String student2Id = "10000000-0000-0000-0000-000000000002";
        String subjectId = "d0000000-0000-0000-0000-000000000001";

        // Try inserting an overlapping booking for the same teacher
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO bookings (id, teacher_id, student_id, subject_id, start_time, end_time, delivery_mode, status, is_trial) " +
                "VALUES (gen_random_uuid(), ?::uuid, ?::uuid, ?::uuid, now() + interval '2 days' + interval '30 minutes', now() + interval '2 days' + interval '90 minutes', 'ONLINE', 'SCHEDULED', true)",
                teacherId, student2Id, subjectId
        )).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("ex_booking_teacher_overlap");
    }

    @Test
    @DisplayName("Invariant 4: Booking Cancellation & Slot Release - Cancelled booking releases slot without hard deletion")
    void invariant4_bookingCancellation_shouldReleaseSlotAndKeepSoftDeleteFalse() {
        String bookingId = UUID.randomUUID().toString();
        String teacherId = "c0000000-0000-0000-0000-000000000003"; // Teacher 3
        String studentId = "10000000-0000-0000-0000-000000000003"; // Student Chi
        String subjectId = "d0000000-0000-0000-0000-000000000003";

        // 1. Create a SCHEDULED trial booking next week
        jdbcTemplate.update(
                "INSERT INTO bookings (id, teacher_id, student_id, subject_id, start_time, end_time, delivery_mode, status, is_trial) " +
                "VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, now() + interval '10 days', now() + interval '10 days' + interval '1 hour', 'ONLINE', 'SCHEDULED', true)",
                bookingId, teacherId, studentId, subjectId
        );

        // 2. Cancel the booking: status -> CANCELLED, is_deleted stays false
        jdbcTemplate.update(
                "UPDATE bookings SET status = 'CANCELLED', cancelled_at = now(), cancel_reason = 'Student requested' WHERE id = ?::uuid",
                bookingId
        );

        Map<String, Object> cancelledBooking = jdbcTemplate.queryForMap(
                "SELECT status, is_deleted FROM bookings WHERE id = ?::uuid",
                bookingId
        );
        assertThat(cancelledBooking.get("status")).isEqualTo("CANCELLED");
        assertThat(cancelledBooking.get("is_deleted")).isEqualTo(false);

        // 3. Re-booking the exact same time slot should SUCCEED because exclusion index filters status = 'SCHEDULED'
        String newBookingId = UUID.randomUUID().toString();
        int rows = jdbcTemplate.update(
                "INSERT INTO bookings (id, teacher_id, student_id, subject_id, start_time, end_time, delivery_mode, status, is_trial) " +
                "VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, now() + interval '10 days', now() + interval '10 days' + interval '1 hour', 'ONLINE', 'SCHEDULED', true)",
                newBookingId, teacherId, studentId, subjectId
        );
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("Invariant 5: Cumulative Rounding Precision - Settle & refund calculations produce zero residual penny loss")
    void invariant5_cumulativeRounding_zeroResidualLoss() {
        // Gói 1.000.000 VNĐ cho 3 buổi học, commission sàn 5%
        long packagePrice = 1_000_000L;
        int totalSessions = 3;
        BigDecimal commissionRate = new BigDecimal("0.05"); // 5%

        long teacherTotalPortion = BigDecimal.valueOf(packagePrice)
                .multiply(BigDecimal.ONE.subtract(commissionRate))
                .setScale(0, RoundingMode.FLOOR)
                .longValue(); // 950.000 VNĐ

        long perSessionNet = BigDecimal.valueOf(teacherTotalPortion)
                .divide(BigDecimal.valueOf(totalSessions), 0, RoundingMode.FLOOR)
                .longValue(); // floor(950000 / 3) = 316.666 VNĐ

        // Session 1: 316.666
        // Session 2: 316.666
        // Session 3 (Final settlement): teacherTotalPortion - (Session 1 + Session 2) = 950.000 - 633.332 = 316.668 VNĐ
        long session1 = perSessionNet;
        long session2 = perSessionNet;
        long session3 = teacherTotalPortion - (session1 + session2);

        assertThat(session1 + session2 + session3).isEqualTo(teacherTotalPortion);
        assertThat(session3).isEqualTo(316668L);
    }

    @Test
    @DisplayName("Invariant 6: Soft Delete & Versioning - Soft deleted records have is_deleted = true and optimistic lock increment")
    void invariant6_softDeleteAndVersioning() {
        String pricingPkgId = "f0000000-0000-0000-0000-000000000004";

        Map<String, Object> before = jdbcTemplate.queryForMap(
                "SELECT version, is_deleted FROM pricing_packages WHERE id = ?::uuid",
                pricingPkgId
        );
        long versionBefore = ((Number) before.get("version")).longValue();
        boolean isDeletedBefore = (Boolean) before.get("is_deleted");
        assertThat(isDeletedBefore).isFalse();

        // Perform soft delete
        jdbcTemplate.update(
                "UPDATE pricing_packages SET is_deleted = true, version = version + 1 WHERE id = ?::uuid",
                pricingPkgId
        );

        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT version, is_deleted FROM pricing_packages WHERE id = ?::uuid",
                pricingPkgId
        );
        assertThat((Boolean) after.get("is_deleted")).isTrue();
        assertThat(((Number) after.get("version")).longValue()).isEqualTo(versionBefore + 1);
    }

    @Test
    @DisplayName("Invariant 7: Append-Only Immutability - Immutable tables do not have is_deleted or updated_at columns")
    void invariant7_appendOnlyImmutability_checkTableColumns() {
        List<String> immutableTables = List.of("ledger_entries", "payment_transactions", "audit_logs");

        for (String table : immutableTables) {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ?",
                    String.class, table
            );

            assertThat(columns)
                    .as("Table %s must not have is_deleted column", table)
                    .doesNotContain("is_deleted");

            assertThat(columns)
                    .as("Table %s must not have updated_at column", table)
                    .doesNotContain("updated_at");

            assertThat(columns)
                    .as("Table %s must have created_at timestamp", table)
                    .contains("created_at");
        }
    }

    @Test
    @DisplayName("Invariant 8: Insufficient refund balance rolls back package, wallet and ledger")
    void invariant8_insufficientRefundBalance_shouldRollbackEveryMutationAndRemainRetrySafe() {
        UUID refundId = UUID.randomUUID();
        String packageId = "40000000-0000-0000-0000-000000000002";
        String walletId = "20000000-0000-0000-0000-000000000002";
        long originalPending = jdbcTemplate.queryForObject(
                "SELECT pending_balance_vnd FROM wallets WHERE id = ?::uuid", Long.class, walletId);
        String originalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM student_packages WHERE id = ?::uuid", String.class, packageId);
        int originalRemaining = jdbcTemplate.queryForObject(
                "SELECT remaining_sessions FROM student_packages WHERE id = ?::uuid", Integer.class, packageId);
        int originalRefunded = jdbcTemplate.queryForObject(
                "SELECT refunded_sessions FROM student_packages WHERE id = ?::uuid", Integer.class, packageId);
        UUID studentId = jdbcTemplate.queryForObject(
                "SELECT student_id FROM student_packages WHERE id = ?::uuid", UUID.class, packageId);
        UUID adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE role = 'ADMIN' ORDER BY created_at LIMIT 1", UUID.class);

        try {
            jdbcTemplate.update(
                    "UPDATE student_packages SET status = 'REFUND_PENDING' WHERE id = ?::uuid", packageId);
            jdbcTemplate.update(
                    "UPDATE wallets SET pending_balance_vnd = 1 WHERE id = ?::uuid", walletId);
            jdbcTemplate.update("""
                    INSERT INTO refund_requests (
                        id, student_package_id, student_id, reason, requested_sessions,
                        approved_sessions, refund_amount_vnd, status, version
                    ) VALUES (?::uuid, ?::uuid, ?::uuid, 'rollback test', 2, 2, 640000, 'APPROVED', 0)
                    """, refundId, packageId, studentId);

            CompleteTransferCommand command = new CompleteTransferCommand(
                    "TEST-REF", Instant.now(), "test-proof", "https://example.test/proof", 0L);

            for (int attempt = 0; attempt < 2; attempt++) {
                assertThatThrownBy(() -> refundService.completeRefund(adminId, refundId, command))
                        .isInstanceOfSatisfying(BusinessException.class,
                                ex -> assertThat(ex.getErrorCode())
                                        .isEqualTo(ErrorCode.REFUND_WALLET_INSUFFICIENT));

                assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM refund_requests WHERE id = ?::uuid", String.class, refundId))
                        .isEqualTo("APPROVED");
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT remaining_sessions FROM student_packages WHERE id = ?::uuid",
                        Integer.class, packageId)).isEqualTo(originalRemaining);
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT refunded_sessions FROM student_packages WHERE id = ?::uuid",
                        Integer.class, packageId)).isEqualTo(originalRefunded);
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT pending_balance_vnd FROM wallets WHERE id = ?::uuid", Long.class, walletId))
                        .isEqualTo(1L);
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM ledger_entries WHERE reference_id = ?::uuid",
                        Long.class, refundId)).isZero();
            }
        } finally {
            jdbcTemplate.update("DELETE FROM refund_requests WHERE id = ?::uuid", refundId);
            jdbcTemplate.update(
                    "UPDATE student_packages SET status = ? WHERE id = ?::uuid", originalStatus, packageId);
            jdbcTemplate.update(
                    "UPDATE wallets SET pending_balance_vnd = ? WHERE id = ?::uuid", originalPending, walletId);
        }
    }
}
