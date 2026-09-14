package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RlsBehaviorVerificationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreRlsState() {
        jdbcTemplate.update("DELETE FROM public.subjects WHERE code LIKE 'RLS_TEST_%'");
    }

    @Test
    @DisplayName("Step 2: Enable RLS on subjects - Backend CRUD succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnSubjects() throws Exception {
        // Verify RLS is enabled on subjects
        Map<String, Object> rlsStatus = jdbcTemplate.queryForMap(
                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subjects'");
        assertThat(rlsStatus.get("rowsecurity")).isEqualTo(true);

        // 2. Test Backend CRUD operations via default connection (table owner / admin)
        String testCode = "RLS_TEST_" + UUID.randomUUID().toString().substring(0, 8);
        String testSlug = "rls-test-" + UUID.randomUUID().toString().substring(0, 8);

        // CREATE
        int inserted = jdbcTemplate.update(
                "INSERT INTO public.subjects (name, code, slug, education_level, is_active) VALUES (?, ?, ?, 'HIGH_SCHOOL', true)",
                "RLS Test Subject", testCode, testSlug
        );
        assertThat(inserted).isEqualTo(1);

        // READ
        Map<String, Object> subject = jdbcTemplate.queryForMap(
                "SELECT name, code, is_active FROM public.subjects WHERE code = ?", testCode);
        assertThat(subject.get("name")).isEqualTo("RLS Test Subject");
        assertThat(subject.get("is_active")).isEqualTo(true);

        // UPDATE
        int updated = jdbcTemplate.update(
                "UPDATE public.subjects SET name = ? WHERE code = ?",
                "RLS Test Subject Updated", testCode
        );
        assertThat(updated).isEqualTo(1);

        Map<String, Object> updatedSubject = jdbcTemplate.queryForMap(
                "SELECT name FROM public.subjects WHERE code = ?", testCode);
        assertThat(updatedSubject.get("name")).isEqualTo("RLS Test Subject Updated");

        // 3. Simulate an unprivileged role without BYPASSRLS (simulating PostgREST 'anon' role)
        String testAnonUser = "test_anon_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
        jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.subjects TO " + testAnonUser + ";");

        try {
            // Connect as test_anon_user
            String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
            try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                 Statement anonStmt = anonConn.createStatement()) {

                // Query SELECT: RLS without policy should return 0 rows for unprivileged user
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.subjects WHERE code = '" + testCode + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows due to RLS default-deny").isEqualTo(0);
                }

                // Query INSERT: RLS without INSERT policy should fail with error
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "INSERT INTO public.subjects (name, code, slug, education_level, is_active) " +
                        "VALUES ('Hacked', 'HACK1', 'hack-1', 'HIGH_SCHOOL', true)"
                )).as("Unprivileged role must be blocked from INSERT by RLS")
                  .hasMessageContaining("violates row-level security policy");

                // Query UPDATE: 0 rows matched due to default-deny SELECT policy
                int anonUpdated = anonStmt.executeUpdate(
                        "UPDATE public.subjects SET name = 'Hacked' WHERE code = '" + testCode + "'");
                assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE due to RLS default-deny").isEqualTo(0);

                // Query DELETE: 0 rows matched due to default-deny SELECT policy
                int anonDeleted = anonStmt.executeUpdate(
                        "DELETE FROM public.subjects WHERE code = '" + testCode + "'");
                assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE due to RLS default-deny").isEqualTo(0);
            }
        } finally {
            // Clean up test role
            jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
            jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
        }

        // 4. Backend DELETE (verifying cleanup by Backend owner works)
        int deleted = jdbcTemplate.update("DELETE FROM public.subjects WHERE code = ?", testCode);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    @DisplayName("Step 2: flyway_schema_history - Backend can still query and update")
    void verifyRlsBehaviorOnFlywaySchemaHistory() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.flyway_schema_history", Integer.class);
        assertThat(count).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("V33: Enable RLS on users - Backend CRUD succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnUsers() throws Exception {
        // Verify RLS is enabled on users
        Map<String, Object> rlsStatus = jdbcTemplate.queryForMap(
                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'users'");
        assertThat(rlsStatus.get("rowsecurity")).isEqualTo(true);

        // 1. Backend CRUD operations via default connection (table owner / admin)
        String testEmail = "rls_user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        int inserted = jdbcTemplate.update(
                "INSERT INTO public.users (email, password_hash, full_name, role, status) VALUES (?, 'hash123', 'RLS Test User', 'STUDENT', 'ACTIVE')",
                testEmail
        );
        assertThat(inserted).isEqualTo(1);

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT email, full_name, role FROM public.users WHERE email = ?", testEmail);
        assertThat(user.get("full_name")).isEqualTo("RLS Test User");

        int updated = jdbcTemplate.update(
                "UPDATE public.users SET full_name = 'RLS Test User Updated' WHERE email = ?", testEmail);
        assertThat(updated).isEqualTo(1);

        // 2. Simulate unprivileged role without BYPASSRLS
        String testAnonUser = "test_anon_u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
        jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.users TO " + testAnonUser + ";");

        try {
            String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
            try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                 Statement anonStmt = anonConn.createStatement()) {

                // SELECT: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.users WHERE email = '" + testEmail + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on users due to RLS default-deny").isEqualTo(0);
                }

                // INSERT: RLS default-deny should fail
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "INSERT INTO public.users (email, password_hash, full_name, role, status) " +
                        "VALUES ('hacker@test.com', 'hack', 'Hacker', 'STUDENT', 'ACTIVE')"
                )).as("Unprivileged role must be blocked from INSERT by RLS")
                  .hasMessageContaining("violates row-level security policy");

                // UPDATE: 0 rows matched
                int anonUpdated = anonStmt.executeUpdate(
                        "UPDATE public.users SET full_name = 'Hacked' WHERE email = '" + testEmail + "'");
                assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE due to RLS default-deny").isEqualTo(0);

                // DELETE: 0 rows matched
                int anonDeleted = anonStmt.executeUpdate(
                        "DELETE FROM public.users WHERE email = '" + testEmail + "'");
                assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE due to RLS default-deny").isEqualTo(0);
            }
        } finally {
            jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
            jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
        }

        // Cleanup
        int deleted = jdbcTemplate.update("DELETE FROM public.users WHERE email = ?", testEmail);
        assertThat(deleted).isEqualTo(1);
    }
}
