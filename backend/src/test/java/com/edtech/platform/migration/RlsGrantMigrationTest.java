package com.edtech.platform.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.DriverManager;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RlsGrantMigrationTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migrationsRevokeExistingGrantsFromSupabaseRoles() throws Exception {
        try (var connection = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("CREATE ROLE anon; CREATE ROLE authenticated");
            Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()).target("33").load().migrate();
            sql.execute("GRANT TRUNCATE ON wallets, teacher_bank_accounts TO anon, authenticated");
            for (String role : List.of("anon", "authenticated")) {
                try (var result = sql.executeQuery("SELECT has_table_privilege('" + role + "', 'wallets', 'TRUNCATE')")) {
                    assertThat(result.next() && result.getBoolean(1)).isTrue();
                }
            }
            sql.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon, authenticated");
            Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()).load().migrate();
            for (String role : List.of("anon", "authenticated")) {
                for (String table : List.of("wallets", "teacher_bank_accounts", "booking_settlements", "platform_ledger_entries")) {
                    var privileges = table.equals("wallets") || table.equals("teacher_bank_accounts")
                            ? List.of("TRUNCATE") : List.of("SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE");
                    for (String privilege : privileges) {
                        try (var result = sql.executeQuery("SELECT has_table_privilege('" + role + "', '" + table + "', '" + privilege + "')")) {
                            assertThat(result.next() && result.getBoolean(1)).as(role + " " + table + " " + privilege).isFalse();
                        }
                    }
                }
            }
        }
    }
}
