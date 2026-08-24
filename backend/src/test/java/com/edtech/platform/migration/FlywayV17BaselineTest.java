package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractPostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayV17BaselineTest extends AbstractPostgresContainerTest {
    @Test
    void v1ToV17RunFromEmptySchemaAndExposeKnownV17Mismatch() {
        String schema = "baseline_" + UUID.randomUUID().toString().replace("-", "");
        String separator = POSTGRES_CONTAINER.getJdbcUrl().contains("?") ? "&" : "?";
        String url = POSTGRES_CONTAINER.getJdbcUrl() + separator + "currentSchema=" + schema;
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());

        Flyway baseline = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .target("17")
                .locations("classpath:db/migration")
                .load();

        baseline.migrate();
        Integer appliedCount = new JdbcTemplate(dataSource).queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true AND version IS NOT NULL",
                Integer.class);
        assertThat(appliedCount).isEqualTo(17);
        assertThat(baseline.info().current().getVersion().getVersion()).isEqualTo("17");
        assertThat(baseline.validateWithResult().validationSuccessful).isTrue();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer deleted = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? " +
                        "AND table_name = 'refresh_tokens' AND column_name = 'deleted'",
                Integer.class, schema);
        Integer isDeleted = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? " +
                        "AND table_name = 'refresh_tokens' AND column_name = 'is_deleted'",
                Integer.class, schema);
        assertThat(deleted).isEqualTo(1);
        assertThat(isDeleted).isEqualTo(1);
    }
}
