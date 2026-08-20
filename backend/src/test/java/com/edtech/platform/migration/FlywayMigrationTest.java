package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf("isDockerAvailable")
public class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Tất cả 16 file migration V1-V16 phải được apply thành công")
    void flyway_shouldApplyAll16MigrationsSuccessfully() {
        assertThat(flyway).isNotNull();
        MigrationInfo[] appliedMigrations = flyway.info().applied();

        assertThat(appliedMigrations)
                .hasSize(16)
                .allSatisfy(info -> {
                    assertThat(info.getState().isApplied()).isTrue();
                    assertThat(info.getVersion()).isNotNull();
                });
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
    @DisplayName("Database phải tạo đủ 29 bảng nghiệp vụ trong public schema")
    void database_shouldContainAll29Tables() {
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
    @DisplayName("V16 phải cập nhật đúng schema cho student_packages, bookings và platform_settings")
    void v16_shouldFixSchemaBugs() {
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
    }
}
