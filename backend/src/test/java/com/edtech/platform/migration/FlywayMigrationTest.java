package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Field;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V31 version columns phải là bigint NOT NULL default 0")
    void optimisticLockColumnsShouldHaveExpectedMetadata() {
        Map<String, String> expected = Map.of(
                "invoices", "version", "trial_requests", "version", "assignments", "version",
                "submissions", "version", "subject_proposals", "version", "teacher_bank_accounts", "version");
        expected.forEach((table, column) -> {
            Map<String, Object> row = jdbcTemplate.queryForMap("SELECT data_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema='public' AND table_name=? AND column_name=?", table, column);
            assertThat(row.get("data_type")).isEqualTo("bigint");
            assertThat(row.get("is_nullable")).isEqualTo("NO");
            assertThat(String.valueOf(row.get("column_default"))).contains("0");
        });
    }

    @Test
    @DisplayName("Sáu entity lõi phải có đúng field JPA @Version")
    void coreEntitiesShouldDeclareVersionField() {
        List<Class<?>> entities = List.of(
                com.edtech.platform.payment.domain.Invoice.class,
                com.edtech.platform.booking.domain.TrialRequest.class,
                com.edtech.platform.learning.domain.Assignment.class,
                com.edtech.platform.learning.domain.Submission.class,
                com.edtech.platform.subject.domain.SubjectProposal.class,
                com.edtech.platform.finance.domain.TeacherBankAccount.class);
        entities.forEach(type -> {
            List<Field> versionFields = java.util.Arrays.stream(type.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Version.class)).toList();
            assertThat(versionFields).as(type.getSimpleName()).hasSize(1);
            assertThat(versionFields.get(0).getName()).isEqualTo("version");
        });
    }

    @Test
    @DisplayName("Tất cả 41 file migration V1-V41 phải được apply và validate thành công")
    void flyway_shouldApplyAllMigrationsSuccessfully() {
        assertThat(flyway).isNotNull();
        MigrationInfo[] appliedMigrations = flyway.info().applied();

        assertThat(appliedMigrations)
                .hasSize(41)
                .allSatisfy(info -> {
                    assertThat(info.getState().isApplied()).isTrue();
                    assertThat(info.getVersion()).isNotNull();
                });
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("V38 phải seed đủ 34 tỉnh và 3.321 xã/phường theo cấu trúc hai cấp")
    void administrativeReferenceDataShouldHaveExpectedCardinality() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM provinces", Integer.class)).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM wards", Integer.class)).isEqualTo(3321);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM wards w LEFT JOIN provinces p ON p.code = w.province_code WHERE p.code IS NULL", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM provinces WHERE code !~ '^[0-9]{2}$'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM wards WHERE code !~ '^[0-9]{5}$'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("V39 phải thêm residence province/ward nullable và FK cùng tỉnh")
    void teacherResidenceColumnsShouldHaveExpectedMetadata() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='teacher_profiles' AND column_name IN ('province_code','ward_code')", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='teacher_profiles' AND column_name IN ('province_code','ward_code') AND is_nullable='YES'", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname IN ('fk_teacher_profiles_province','fk_teacher_profiles_ward','ck_teacher_profiles_residence_ward_requires_province')", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("Các extension PostgreSQL bắt buộc phải tồn tại")
    void database_shouldHaveRequiredExtensions() {
        assertThat(jdbcTemplate).isNotNull();
        List<String> extensions = jdbcTemplate.query(
                "SELECT extname FROM pg_extension WHERE extname IN ('pgcrypto', 'btree_gist', 'unaccent', 'pg_trgm')",
                (rs, rowNum) -> rs.getString("extname")
        );

        assertThat(extensions).contains("pgcrypto", "btree_gist", "unaccent", "pg_trgm");
    }

    @Test
    @DisplayName("V28 phải tạo immutable search normalizer và các index marketplace")
    void teacherSearchOptimizationShouldExposeExpectedMetadata() {
        assertThat(jdbcTemplate).isNotNull();

        String volatility = jdbcTemplate.queryForObject("""
                SELECT p.provolatile::text
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'public' AND p.proname = 'f_unaccent_immutable'
                """, String.class);
        assertThat(volatility).isEqualTo("i");

        List<String> indexes = jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public' AND indexname IN (
                    'ix_users_search_full_name_trgm',
                    'ix_teacher_profiles_search_bio_trgm',
                    'ix_pricing_packages_active_teacher_price'
                )
                """, String.class);
        assertThat(indexes).containsExactlyInAnyOrder(
                "ix_users_search_full_name_trgm",
                "ix_teacher_profiles_search_bio_trgm",
                "ix_pricing_packages_active_teacher_price");
    }

    @Test
    @DisplayName("V29 phải tạo finance receipts và trạng thái workflow tối giản")
    void financeHardeningMigrationShouldExposeExpectedMetadata() {
        assertThat(jdbcTemplate).isNotNull();
        Integer transferredAt = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' " +
                        "AND table_name = 'refund_requests' AND column_name = 'transferred_at'",
                Integer.class);
        assertThat(transferredAt).isEqualTo(1);
        Integer receipts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' " +
                        "AND table_name = 'finance_command_receipts'",
                Integer.class);
        assertThat(receipts).isEqualTo(1);
        List<String> indexes = jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public' AND indexname IN (
                  'ux_refund_requests_active_package',
                  'ux_extension_requests_pending_package',
                  'ux_payout_requests_active_teacher'
                )
                """, String.class);
        assertThat(indexes).containsExactlyInAnyOrder(
                "ux_refund_requests_active_package",
                "ux_extension_requests_pending_package",
                "ux_payout_requests_active_teacher");
    }

    @Test
    @DisplayName("Database đang ở V27 phải nâng cấp lên V31 thành công")
    void teacherSearchMigrationShouldUpgradeASeparateV27Database() {
        String databaseName = "edtech_v27_upgrade";
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(),
                "/" + databaseName
        );

        Flyway v27 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("27")
                .load();
        assertThat(v27.migrate().targetSchemaVersion.toString()).isEqualTo("27");

        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("V31 terminal finance rows phải có transfer proof constraints")
    void financeTerminalProofConstraintsShouldExist() {
        List<String> constraints = jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conname IN ('ck_payout_succeeded_transfer_proof', 'ck_refund_refunded_transfer_proof')
                  AND connamespace = current_schema()::regnamespace
                """, String.class);
        assertThat(constraints).containsExactlyInAnyOrder(
                "ck_payout_succeeded_transfer_proof", "ck_refund_refunded_transfer_proof");
    }

    @Test
    @DisplayName("Database đang ở V30 phải nâng cấp riêng lên V38")
    void v30DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v30_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v30 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("30").load();
        assertThat(v30.migrate().targetSchemaVersion.toString()).isEqualTo("30");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V31 phải nâng cấp riêng lên V38")
    void v31DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v31_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v31 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("31").load();
        assertThat(v31.migrate().targetSchemaVersion.toString()).isEqualTo("31");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V32 phải nâng cấp riêng lên V38")
    void v32DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v32_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v32 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("32").load();
        assertThat(v32.migrate().targetSchemaVersion.toString()).isEqualTo("32");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V33 phải nâng cấp riêng lên V38")
    void v33DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v33_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v33 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("33").load();
        assertThat(v33.migrate().targetSchemaVersion.toString()).isEqualTo("33");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V34 phải nâng cấp riêng lên V38")
    void v34DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v34_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v34 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("34").load();
        assertThat(v34.migrate().targetSchemaVersion.toString()).isEqualTo("34");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V35 phải nâng cấp riêng lên V38")
    void v35DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v35_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v35 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("35").load();
        assertThat(v35.migrate().targetSchemaVersion.toString()).isEqualTo("35");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V36 phải nâng cấp riêng lên V38")
    void v36DatabaseShouldUpgradeToV37() {
        String databaseName = "edtech_v36_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v36 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("36").load();
        assertThat(v36.migrate().targetSchemaVersion.toString()).isEqualTo("36");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("Database đang ở V37 phải nâng cấp riêng lên V40")
    void v37DatabaseShouldUpgradeToV40() {
        String databaseName = "edtech_v37_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v37 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("37").load();
        assertThat(v37.migrate().targetSchemaVersion.toString()).isEqualTo("37");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("V40 phải bật RLS default-deny cho teacher_credentials")
    void teacherCredentialsShouldHaveRlsEnabled() {
        Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'teacher_credentials'", Boolean.class);
        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("Production V40 phải nâng cấp sạch lên V41")
    void v40DatabaseShouldUpgradeToV41() {
        String databaseName = "edtech_v40_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = POSTGRES_CONTAINER.getJdbcUrl().replace(
                "/" + POSTGRES_CONTAINER.getDatabaseName(), "/" + databaseName);
        Flyway v40 = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                .target("40").load();
        assertThat(v40.migrate().targetSchemaVersion.toString()).isEqualTo("40");
        Flyway latest = Flyway.configure()
                .dataSource(upgradeUrl, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword()).load();
        assertThat(latest.migrate().targetSchemaVersion.toString()).isEqualTo("41");
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("V41 tạo escrow, settlement, ledger allow-list và RLS default-deny")
    void bookingEscrowSchemaShouldBeSafe() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema='public' AND table_name IN ('booking_settlements','platform_ledger_entries')
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema='public' AND table_name='booking_settlements' AND column_name='net_amount_vnd'
                """, String.class)).isEqualTo("YES");
        assertThat(jdbcTemplate.queryForList("""
                SELECT tablename FROM pg_tables WHERE schemaname='public'
                  AND tablename IN ('booking_settlements','platform_ledger_entries') AND rowsecurity
                """, String.class)).containsExactlyInAnyOrder("booking_settlements", "platform_ledger_entries");
        String ledgerConstraint = jdbcTemplate.queryForObject("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conname='ck_ledger_entries_entry_type'
                """, String.class);
        assertThat(ledgerConstraint).contains("SESSION_ESCROW_HELD", "SESSION_ESCROW_RELEASED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema='public' AND table_name='ledger_entries' AND column_name='entry_type'
                """, Integer.class)).isEqualTo(30);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_trigger WHERE tgname='trg_platform_ledger_append_only'
                """, Integer.class)).isEqualTo(1);
        for (String role : List.of("anon", "authenticated")) {
            boolean exists = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=?)", Boolean.class, role));
            if (exists) {
                for (String table : List.of("booking_settlements", "platform_ledger_entries")) {
                    for (String privilege : List.of("SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE")) {
                        Boolean granted = jdbcTemplate.queryForObject(
                                "SELECT has_table_privilege(?, ?, ?)", Boolean.class, role, table, privilege);
                        assertThat(granted).as(role + " " + privilege + " " + table).isFalse();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("RLS phải bật default-deny trên 34 bảng Backend-owned")
    void rlsShouldBeEnabledOnBackendOwnedTables() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT tablename
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename IN (
                    'users', 'refresh_tokens', 'teacher_documents', 'teacher_credentials',
                    'subject_proposals', 'subjects', 'pricing_packages',
                    'teacher_profiles', 'teacher_subjects', 'teacher_availabilities',
                    'wallets', 'ledger_entries', 'invoices', 'payment_transactions',
                    'payout_requests', 'refund_requests', 'teacher_bank_accounts', 'finance_command_receipts',
                    'student_packages', 'package_extension_requests', 'trial_requests',
                    'bookings', 'session_reports', 'reviews', 'teacher_stats',
                    'assignments', 'submissions',
                    'conversations', 'messages', 'attachments', 'notifications',
                    'platform_settings', 'audit_logs', 'email_outbox'
                  )
                  AND rowsecurity = true
                ORDER BY tablename
                """, String.class);
        assertThat(tables).containsExactlyInAnyOrder(
                "users", "refresh_tokens", "teacher_documents", "teacher_credentials",
                "subject_proposals", "subjects", "pricing_packages",
                "teacher_profiles", "teacher_subjects", "teacher_availabilities",
                "wallets", "ledger_entries", "invoices", "payment_transactions",
                "payout_requests", "refund_requests", "teacher_bank_accounts", "finance_command_receipts",
                "student_packages", "package_extension_requests", "trial_requests",
                "bookings", "session_reports", "reviews", "teacher_stats",
                "assignments", "submissions",
                "conversations", "messages", "attachments", "notifications",
                "platform_settings", "audit_logs", "email_outbox");

        Long policyCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_policy p
                JOIN pg_class c ON c.oid = p.polrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relname IN (
                    'users', 'refresh_tokens', 'teacher_documents',
                    'subject_proposals', 'subjects', 'pricing_packages',
                    'teacher_profiles', 'teacher_subjects', 'teacher_availabilities',
                    'wallets', 'ledger_entries', 'invoices', 'payment_transactions',
                    'payout_requests', 'refund_requests', 'teacher_bank_accounts', 'finance_command_receipts',
                    'student_packages', 'package_extension_requests', 'trial_requests',
                    'bookings', 'session_reports', 'reviews', 'teacher_stats',
                    'assignments', 'submissions',
                    'conversations', 'messages', 'attachments', 'notifications',
                    'platform_settings', 'audit_logs', 'email_outbox'
                  )
                """, Long.class);
        assertThat(policyCount).isZero();
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
                "SELECT conname FROM pg_constraint WHERE connamespace = 'public'::regnamespace AND conname IN ('ex_booking_teacher_overlap', 'ex_booking_student_overlap')",
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
                "SELECT numeric_precision FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'student_packages' AND column_name = 'commission_rate'",
                Integer.class
        );
        Integer scale = jdbcTemplate.queryForObject(
                "SELECT numeric_scale FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'student_packages' AND column_name = 'commission_rate'",
                Integer.class
        );
        assertThat(precision).isEqualTo(5);
        assertThat(scale).isEqualTo(2);

        // 2. Check platform_settings has is_singleton
        Integer singletonCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'platform_settings' AND column_name = 'is_singleton'",
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
                WHERE connamespace = 'public'::regnamespace AND conname IN (
                  'uq_invoices_student_idempotency', 'ck_invoices_amount_positive',
                  'ck_payment_transactions_amount_positive', 'ck_student_packages_total_positive',
                  'ck_student_packages_price_positive', 'ck_student_packages_commission_range',
                  'ck_ledger_entries_balance_bucket', 'ck_ledger_entries_entry_type'
                )
                """, String.class);
        assertThat(constraints).hasSize(8);
    }
}
