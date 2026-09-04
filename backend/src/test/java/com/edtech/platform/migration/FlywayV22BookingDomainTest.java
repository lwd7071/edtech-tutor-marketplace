package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractPostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayV22BookingDomainTest extends AbstractPostgresContainerTest {

    @Test
    void migrationV22_shouldApplyConstraintsAndIndexesSuccessfully() {
        String schema = "v22_" + UUID.randomUUID().toString().replace("-", "");
        String separator = POSTGRES_CONTAINER.getJdbcUrl().contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES_CONTAINER.getJdbcUrl() + separator + "currentSchema=" + schema,
                POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Migrate all migrations up to V22
        Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load();
        flyway.migrate();

        jdbc.execute("SET search_path TO " + schema + ", public");

        // Verify trial uniqueness partial index
        UUID studentId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                studentId, studentId + "@example.test", "hash", "Student", "STUDENT", "ACTIVE");
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                teacherUserId, teacherUserId + "@example.test", "hash", "Teacher", "TEACHER", "ACTIVE");
        jdbc.update("INSERT INTO teacher_profiles (id,user_id,profile_status) VALUES (?,?,?)",
                teacherId, teacherUserId, "APPROVED");
        jdbc.update("INSERT INTO subjects (id,code,name,slug) VALUES (?,?,?,?)",
                subjectId, "SUB-" + subjectId, "Subject", "subject-" + subjectId);

        Instant start1 = Instant.parse("2026-09-05T10:00:00Z");
        Instant end1 = Instant.parse("2026-09-05T11:00:00Z");
        Instant start2 = Instant.parse("2026-09-06T10:00:00Z");
        Instant end2 = Instant.parse("2026-09-06T11:00:00Z");

        // Insert first trial booking
        jdbc.update("""
                INSERT INTO bookings (id, teacher_id, student_id, subject_id, start_time, end_time, delivery_mode, status, is_trial)
                VALUES (?, ?, ?, ?, ?, ?, 'ONLINE', 'SCHEDULED', true)
                """, UUID.randomUUID(), teacherId, studentId, subjectId, java.sql.Timestamp.from(start1), java.sql.Timestamp.from(end1));

        // Inserting second trial booking in SCHEDULED for same pair should violate ux_bookings_trial_pair
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO bookings (id, teacher_id, student_id, subject_id, start_time, end_time, delivery_mode, status, is_trial)
                VALUES (?, ?, ?, ?, ?, ?, 'ONLINE', 'SCHEDULED', true)
                """, UUID.randomUUID(), teacherId, studentId, subjectId, java.sql.Timestamp.from(start2), java.sql.Timestamp.from(end2)))
                .isNotNull();
    }

    private Flyway configure(DriverManagerDataSource dataSource, String schema, String target) {
        var config = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }
}