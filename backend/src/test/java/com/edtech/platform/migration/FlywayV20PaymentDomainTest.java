package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractPostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayV20PaymentDomainTest extends AbstractPostgresContainerTest {

    @Test
    void legacyInvoiceGetsExactDeterministicFingerprint() {
        String schema = "v20_" + UUID.randomUUID().toString().replace("-", "");
        String separator = POSTGRES_CONTAINER.getJdbcUrl().contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES_CONTAINER.getJdbcUrl() + separator + "currentSchema=" + schema,
                POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway baseline = configure(dataSource, schema, "19");
        baseline.migrate();

        UUID studentId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID pricingId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-27T01:02:03.123456Z");
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                studentId, studentId + "@example.test", "hash", "Student", "STUDENT", "ACTIVE");
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                teacherUserId, teacherUserId + "@example.test", "hash", "Teacher", "TEACHER", "ACTIVE");
        jdbc.update("INSERT INTO teacher_profiles (id,user_id,profile_status) VALUES (?,?,?)",
                teacherId, teacherUserId, "APPROVED");
        jdbc.update("INSERT INTO subjects (id,code,name,slug) VALUES (?,?,?,?)",
                subjectId, "SUB-" + subjectId, "Subject", "subject-" + subjectId);
        jdbc.update("INSERT INTO pricing_packages (id,teacher_id,subject_id,name,total_sessions,duration_days,price_vnd,session_duration_minutes,status) VALUES (?,?,?,?,?,?,?,?,?)",
                pricingId, teacherId, subjectId, "Package", 1, 30, 100_000L, 60, "ACTIVE");
        jdbc.update("INSERT INTO invoices (id,invoice_number,student_id,teacher_id,pricing_package_id,amount_vnd,status,created_at) VALUES (?,?,?,?,?,?,?,?)",
                invoiceId, "INV-LEGACY", studentId, teacherId, pricingId, 100_000L, "PENDING",
                java.sql.Timestamp.from(createdAt));

        configure(dataSource, schema, null).migrate();

        String expected = jdbc.queryForObject("""
                SELECT encode(digest(id::text || '|' || amount_vnd::text || '|' ||
                    to_char(created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
                    'sha256'), 'hex') FROM invoices WHERE id = ?
                """, String.class, invoiceId);
        assertThat(jdbc.queryForObject(
                "SELECT request_fingerprint FROM invoices WHERE id = ?", String.class, invoiceId))
                .isEqualTo(expected);
        assertThat(jdbc.queryForObject(
                "SELECT idempotency_key FROM invoices WHERE id = ?", UUID.class, invoiceId))
                .isEqualTo(invoiceId);
    }

    private Flyway configure(DriverManagerDataSource dataSource, String schema, String target) {
        var config = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }
}
