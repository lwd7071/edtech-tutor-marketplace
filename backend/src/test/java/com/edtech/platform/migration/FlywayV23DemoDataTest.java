package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractPostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayV23DemoDataTest extends AbstractPostgresContainerTest {

    @Test
    void migrationV23_shouldSeedDemoDataWithoutConstraintViolations() {
        String schema = "v23_" + UUID.randomUUID().toString().replace("-", "");
        String separator = POSTGRES_CONTAINER.getJdbcUrl().contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES_CONTAINER.getJdbcUrl() + separator + "currentSchema=" + schema,
                POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load();
        flyway.migrate();

        jdbc.execute("SET search_path TO " + schema + ", public");

        // 1. Check Admin
        Integer adminCount = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class);
        assertThat(adminCount).isGreaterThanOrEqualTo(1);

        // 2. Check Teachers
        Integer teacherCount = jdbc.queryForObject("SELECT COUNT(*) FROM teacher_profiles WHERE profile_status = 'APPROVED'", Integer.class);
        assertThat(teacherCount).isGreaterThanOrEqualTo(3);

        // 3. Check Subjects
        Integer subjectCount = jdbc.queryForObject("SELECT COUNT(*) FROM subjects WHERE is_active = true", Integer.class);
        assertThat(subjectCount).isGreaterThanOrEqualTo(3);

        // 4. Check Packages
        Integer pricingCount = jdbc.queryForObject("SELECT COUNT(*) FROM pricing_packages WHERE status = 'ACTIVE'", Integer.class);
        assertThat(pricingCount).isGreaterThanOrEqualTo(4);

        // 5. Check Student Packages & Bookings
        Integer studentPkgCount = jdbc.queryForObject("SELECT COUNT(*) FROM student_packages WHERE status = 'ACTIVE'", Integer.class);
        assertThat(studentPkgCount).isGreaterThanOrEqualTo(2);

        Integer bookingCount = jdbc.queryForObject("SELECT COUNT(*) FROM bookings", Integer.class);
        assertThat(bookingCount).isGreaterThanOrEqualTo(2);

        // 6. Check Ledger entries
        Integer ledgerCount = jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
        assertThat(ledgerCount).isGreaterThanOrEqualTo(4);
    }
}