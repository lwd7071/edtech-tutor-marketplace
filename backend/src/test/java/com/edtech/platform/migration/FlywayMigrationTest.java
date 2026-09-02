package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Tất cả 20 file migration V1-V20 phải được apply và validate thành công")
    void flyway_shouldApplyAllMigrationsSuccessfully() {
        assertThat(flyway).isNotNull();
        MigrationInfo[] appliedMigrations = flyway.info().applied();

        assertThat(appliedMigrations)
                .hasSize(20)
                .allSatisfy(info -> {
                    assertThat(info.getState().isApplied()).isTrue();
                    assertThat(info.getVersion()).isNotNull();
                });
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Các extension pgcrypto và btree_gist phải tồn tại")
    void database_shouldHaveRequiredExtensions() {
        assertThat(jdbcTemplate).isNotNull();
        List<String> extensions = jdbcTemplate.query(
                "SELECT extname FROM pg_extension WHERE extname IN ('pgcrypto', 'btree_gist')",
                (rs, rowNum) -> rs.getString("extname")
        );

        assertThat(extensions).contains("pgcrypto", "btree_gist");
    }

    @Test
    @DisplayName("Trigger function set_updated_at phải tồn tại")
    void database_shouldHaveSetUpdatedAtFunction() {
        assertThat(jdbcTemplate).isNotNull();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_proc WHERE proname = 'set_updated_at'",
                Integer.class
        );

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Bảng bookings phải có exclusion constraints chống trùng lịch GiST")
    void bookings_shouldHaveExclusionConstraints() {
        assertThat(jdbcTemplate).isNotNull();
        List<String> constraints = jdbcTemplate.query(
                "SELECT conname FROM pg_constraint WHERE conname IN ('ex_booking_teacher_overlap', 'ex_booking_student_overlap')",
                (rs, rowNum) -> rs.getString("conname")
        );

        assertThat(constraints).contains("ex_booking_teacher_overlap", "ex_booking_student_overlap");
    }

    @Test
    @DisplayName("Database phải tạo đủ 31 bảng nghiệp vụ trong public schema")
    void database_shouldContainAllTables() {
        assertThat(jdbcTemplate).isNotNull();
        List<String> tables = jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name != 'flyway_schema_history'",
                (rs, rowNum) -> rs.getString("table_name")
        );

        List<String> expectedTables = List.of(
                "users",
                "refresh_tokens",
                "teacher_profiles",
                "teacher_documents",
                "teacher_availabilities",
                "subjects",
                "subject_proposals",
                "teacher_subjects",
                "pricing_packages",
                "invoices",
                "payment_transactions",
                "student_packages",
                "bookings",
                "trial_requests",
                "session_reports",
                "reviews",
                "teacher_stats",
                "wallets",
                "ledger_entries",
                "teacher_bank_accounts",
                "payout_requests",
                "refund_requests",
                "package_extension_requests",
                "assignments",
                "submissions",
                "attachments",
                "conversations",
                "messages",
                "notifications",
                "audit_logs",
                "platform_settings"
        );

        assertThat(tables).containsAll(expectedTables);
    }
    @Test
    @DisplayName("Metadata V16-V18 phải khớp entity, FK và business invariants")
    void hardeningMigrationsShouldExposeExpectedMetadata() {
        assertThat(jdbcTemplate).isNotNull();

        // 1. Check student_packages.commission_rate is numeric(5,2)
        Integer precision = jdbcTemplate.queryForObject(
                "SELECT numeric_precision FROM information_schema.columns WHERE table_name = 'student_packages' AND column_name = 'commission_rate'",
                Integer.class
        );
        Integer scale = jdbcTemplate.queryForObject(
                "SELECT numeric_scale FROM information_schema.columns WHERE table_name = 'student_packages' AND column_name = 'commission_rate'",
                Integer.class
        );
        assertThat(precision).isEqualTo(5);
        assertThat(scale).isEqualTo(2);

        // 2. Check platform_settings has is_singleton
        Integer singletonCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'platform_settings' AND column_name = 'is_singleton'",
                Integer.class
        );
        assertThat(singletonCount).isEqualTo(1);

        // 3. Check bookings status constraint exists
        Integer checkConstraintCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conrelid = 'bookings'::regclass AND contype = 'c' AND conname = 'ck_bookings_status'",
                Integer.class
        );
        assertThat(checkConstraintCount).isGreaterThanOrEqualTo(1);

        Integer refreshSoftDelete = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' " +
                        "AND table_name = 'refresh_tokens' AND column_name = 'is_deleted'",
                Integer.class);
        Integer staleDeleted = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' " +
                        "AND table_name = 'refresh_tokens' AND column_name = 'deleted'",
                Integer.class);
        assertThat(refreshSoftDelete).isEqualTo(1);
        assertThat(staleDeleted).isZero();

        List<java.util.Map<String, Object>> teacherForeignKeys = jdbcTemplate.queryForList("""
                SELECT tc.table_name, ccu.table_name AS foreign_table, ccu.column_name AS foreign_column
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name AND tc.constraint_schema = kcu.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = tc.constraint_name AND ccu.constraint_schema = tc.constraint_schema
                WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
                  AND kcu.column_name = 'teacher_id'
                """);
        assertThat(teacherForeignKeys).isNotEmpty().allSatisfy(fk -> {
            assertThat(fk.get("foreign_table")).isEqualTo("teacher_profiles");
            assertThat(fk.get("foreign_column")).isEqualTo("id");
        });

        List<String> singletonConstraints = jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'platform_settings'::regclass " +
                        "AND conname IN ('uq_platform_settings_singleton','ck_platform_settings_singleton')",
                String.class);
        assertThat(singletonConstraints).containsExactlyInAnyOrder(
                "uq_platform_settings_singleton", "ck_platform_settings_singleton");
    }

    @Test
    @DisplayName("V20 phải khóa idempotency, sequence và payment constraints")
    void paymentDomainMigrationShouldExposeExpectedMetadata() {
        assertThat(jdbcTemplate).isNotNull();

        List<String> invoiceColumns = jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'invoices'
                  AND column_name IN ('idempotency_key', 'request_fingerprint')
                  AND is_nullable = 'NO'
                """, String.class);
        assertThat(invoiceColumns).containsExactlyInAnyOrder("idempotency_key", "request_fingerprint");

        List<String> sequences = jdbcTemplate.queryForList("""
                SELECT sequence_name FROM information_schema.sequences
                WHERE sequence_schema = 'public'
                  AND sequence_name IN ('invoice_number_seq', 'payos_order_code_seq')
                """, String.class);
        assertThat(sequences).containsExactlyInAnyOrder("invoice_number_seq", "payos_order_code_seq");

        List<String> constraints = jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conname IN (
                  'uq_invoices_student_idempotency', 'ck_invoices_amount_positive',
                  'ck_payment_transactions_amount_positive', 'ck_student_packages_total_positive',
                  'ck_student_packages_price_positive', 'ck_student_packages_commission_range',
                  'ck_ledger_entries_balance_bucket', 'ck_ledger_entries_entry_type'
                )
                """, String.class);
        assertThat(constraints).hasSize(8);
    }
}
