package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractPostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayV18LegacyDataTest extends AbstractPostgresContainerTest {
    @Test
    void knownTeacherLegacyValueIsMappedAndSingletonIsSeeded() {
        TestSchema schema = migrateToV17();
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        schema.jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                userId, userId + "@example.test", "hash", "Teacher", "TEACHER", "ACTIVE");
        schema.jdbc.update("INSERT INTO teacher_profiles (id,user_id,profile_status) VALUES (?,?,?)",
                teacherId, userId, "PENDING");

        Flyway full = flyway(schema, null);
        full.migrate();

        assertThat(schema.jdbc.queryForObject(
                "SELECT profile_status FROM teacher_profiles WHERE id = ?", String.class, teacherId))
                .isEqualTo("PENDING_APPROVAL");
        assertThat(schema.jdbc.queryForObject("SELECT count(*) FROM platform_settings", Integer.class)).isEqualTo(1);
        assertThat(schema.jdbc.queryForObject(
                "SELECT commission_rate FROM platform_settings", java.math.BigDecimal.class))
                .isEqualByComparingTo("5.00");
    }

    @ParameterizedTest(name = "{0}.{1} -> {2}")
    @MethodSource("hardenedColumns")
    void unknownLegacyValueFailsWithTableConstraintAndRowCount(
            String table, String column, String constraint) {
        TestSchema schema = migrateToV17();
        insertValidLegacyFixture(schema.jdbc);
        schema.jdbc.update("UPDATE " + table + " SET " + column
                + " = 'UNKNOWN' WHERE ctid = (SELECT ctid FROM " + table + " LIMIT 1)");

        assertThatThrownBy(() -> flyway(schema, null).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("table=" + table)
                .hasMessageContaining("constraint=" + constraint)
                .hasMessageContaining("invalid_rows=1");
    }

    @Test
    void nonDefaultExistingCommissionSnapshotFailsWithoutCoercion() {
        TestSchema schema = migrateToV17();
        insertValidLegacyFixture(schema.jdbc);
        schema.jdbc.update("UPDATE student_packages SET commission_rate = 7.00");

        assertThatThrownBy(() -> flyway(schema, null).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("table=student_packages")
                .hasMessageContaining("constraint=commission_snapshot_source")
                .hasMessageContaining("invalid_rows=1");
    }

    private static Stream<Arguments> hardenedColumns() {
        return Stream.of(
                Arguments.of("users", "status", "ck_users_status"),
                Arguments.of("teacher_profiles", "profile_status", "ck_teacher_profiles_status"),
                Arguments.of("teacher_documents", "verification_status", "ck_teacher_documents_status"),
                Arguments.of("subject_proposals", "status", "ck_subject_proposals_status"),
                Arguments.of("pricing_packages", "status", "ck_pricing_packages_status"),
                Arguments.of("invoices", "status", "ck_invoices_status"),
                Arguments.of("student_packages", "status", "ck_student_packages_status"),
                Arguments.of("payout_requests", "status", "ck_payout_requests_status"),
                Arguments.of("refund_requests", "status", "ck_refund_requests_status"),
                Arguments.of("package_extension_requests", "status", "ck_package_extension_requests_status"),
                Arguments.of("assignments", "assignment_type", "ck_assignments_type"),
                Arguments.of("assignments", "status", "ck_assignments_status"),
                Arguments.of("submissions", "status", "ck_submissions_status"),
                Arguments.of("messages", "message_type", "ck_messages_type")
        );
    }

    private void insertValidLegacyFixture(JdbcTemplate jdbc) {
        UUID studentId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID pricingId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID studentPackageId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID bankId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                studentId, studentId + "@example.test", "hash", "Student", "STUDENT", "ACTIVE");
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                teacherUserId, teacherUserId + "@example.test", "hash", "Teacher", "TEACHER", "ACTIVE");
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                adminId, adminId + "@example.test", "hash", "Admin", "ADMIN", "ACTIVE");
        jdbc.update("INSERT INTO teacher_profiles (id,user_id,profile_status) VALUES (?,?,?)",
                teacherId, teacherUserId, "DRAFT");
        jdbc.update("INSERT INTO teacher_documents (id,teacher_id,document_type,title,secure_url,verification_status) VALUES (?,?,?,?,?,?)",
                documentId, teacherId, "CERTIFICATE", "Document", "https://example.test/document", "PENDING");
        jdbc.update("INSERT INTO subjects (id,code,name,slug) VALUES (?,?,?,?)",
                subjectId, "SUB-" + subjectId, "Subject", "subject-" + subjectId);
        jdbc.update("INSERT INTO subject_proposals (id,teacher_id,proposed_name,status) VALUES (?,?,?,?)",
                proposalId, teacherId, "Proposal", "PENDING");
        jdbc.update("INSERT INTO pricing_packages (id,teacher_id,subject_id,name,total_sessions,duration_days,price_vnd,session_duration_minutes,status) VALUES (?,?,?,?,?,?,?,?,?)",
                pricingId, teacherId, subjectId, "Package", 1, 30, 100_000L, 60, "ACTIVE");
        jdbc.update("INSERT INTO invoices (id,invoice_number,student_id,teacher_id,pricing_package_id,amount_vnd,status) VALUES (?,?,?,?,?,?,?)",
                invoiceId, "INV-" + invoiceId, studentId, teacherId, pricingId, 100_000L, "PENDING");
        jdbc.update("INSERT INTO student_packages (id,student_id,teacher_id,subject_id,pricing_package_id,invoice_id,package_name_snapshot,total_sessions,remaining_sessions,purchase_price_vnd,commission_rate,starts_at,expires_at,status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                studentPackageId, studentId, teacherId, subjectId, pricingId, invoiceId, "Package", 1, 1,
                100_000L, null, java.sql.Timestamp.from(java.time.Instant.now()),
                java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(86400)), "ACTIVE");
        jdbc.update("INSERT INTO wallets (id,teacher_id) VALUES (?,?)", walletId, teacherId);
        jdbc.update("INSERT INTO teacher_bank_accounts (id,teacher_id,bank_bin,bank_name,account_number_encrypted,account_holder_name) VALUES (?,?,?,?,?,?)",
                bankId, teacherId, "970000", "Bank", "encrypted", "TEACHER");
        jdbc.update("INSERT INTO payout_requests (teacher_id,wallet_id,bank_account_id,amount_vnd,status) VALUES (?,?,?,?,?)",
                teacherId, walletId, bankId, 1_000L, "PENDING");
        jdbc.update("INSERT INTO refund_requests (student_package_id,student_id,requested_sessions,status) VALUES (?,?,?,?)",
                studentPackageId, studentId, 1, "PENDING");
        jdbc.update("INSERT INTO package_extension_requests (student_package_id,student_id,requested_expiry_date,status) VALUES (?,?,?,?)",
                studentPackageId, studentId,
                java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(172800)), "PENDING");
        jdbc.update("INSERT INTO assignments (id,teacher_id,student_id,subject_id,title,assignment_type,status) VALUES (?,?,?,?,?,?,?)",
                assignmentId, teacherId, studentId, subjectId, "Assignment", "SYSTEM_QUIZ", "DRAFT");
        jdbc.update("INSERT INTO submissions (assignment_id,student_id,status) VALUES (?,?,?)",
                assignmentId, studentId, "DRAFT");
        jdbc.update("INSERT INTO conversations (id,teacher_id,student_id) VALUES (?,?,?)",
                conversationId, teacherId, studentId);
        jdbc.update("INSERT INTO messages (conversation_id,sender_id,client_message_id,message_type,content) VALUES (?,?,?,?,?)",
                conversationId, studentId, UUID.randomUUID(), "TEXT", "Hello");
    }

    private TestSchema migrateToV17() {
        String name = "legacy_" + UUID.randomUUID().toString().replace("-", "");
        String separator = POSTGRES_CONTAINER.getJdbcUrl().contains("?") ? "&" : "?";
        String url = POSTGRES_CONTAINER.getJdbcUrl() + separator + "currentSchema=" + name;
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());
        TestSchema schema = new TestSchema(name, dataSource, new JdbcTemplate(dataSource), null);
        Flyway baseline = flyway(schema, "17");
        baseline.migrate();
        return new TestSchema(name, dataSource, schema.jdbc, baseline);
    }

    private Flyway flyway(TestSchema schema, String target) {
        org.flywaydb.core.api.configuration.FluentConfiguration config = Flyway.configure()
                .dataSource(schema.dataSource)
                .schemas(schema.name)
                .defaultSchema(schema.name)
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private record TestSchema(String name, DriverManagerDataSource dataSource, JdbcTemplate jdbc, Flyway flyway) {
    }
}
