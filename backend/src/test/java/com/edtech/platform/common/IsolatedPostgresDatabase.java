package com.edtech.platform.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

/**
 * Gives historical Flyway tests a database of their own so CREATE EXTENSION
 * statements cannot pollute the public schema used by Spring integration tests.
 */
public final class IsolatedPostgresDatabase implements AutoCloseable {
    private final String databaseName;
    private final String jdbcUrlPrefix;
    private final String username;
    private final String password;

    private IsolatedPostgresDatabase(String databaseName, String jdbcUrlPrefix,
                                     String username, String password) {
        this.databaseName = databaseName;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        this.username = username;
        this.password = password;
    }

    public static IsolatedPostgresDatabase create(PostgreSQLContainer<?> container) {
        String containerJdbcUrl = container.getJdbcUrl();
        String jdbcUrlPrefix = containerJdbcUrl.substring(0, containerJdbcUrl.lastIndexOf('/') + 1);
        String databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource adminDataSource = new DriverManagerDataSource(
                jdbcUrlPrefix + "postgres", container.getUsername(), container.getPassword());
        new JdbcTemplate(adminDataSource).execute("CREATE DATABASE \"" + databaseName + "\"");
        return new IsolatedPostgresDatabase(
                databaseName, jdbcUrlPrefix, container.getUsername(), container.getPassword());
    }

    public String jdbcUrl() {
        return jdbcUrlPrefix + databaseName;
    }

    public DriverManagerDataSource dataSource(String schema) {
        return new DriverManagerDataSource(
                jdbcUrl() + "?currentSchema=" + schema, username, password);
    }

    @Override
    public void close() {
        DriverManagerDataSource adminDataSource = new DriverManagerDataSource(
                jdbcUrlPrefix + "postgres", username, password);
        JdbcTemplate jdbc = new JdbcTemplate(adminDataSource);
        jdbc.queryForList("SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                "WHERE datname = ? AND pid <> pg_backend_pid()", databaseName);
        jdbc.execute("DROP DATABASE \"" + databaseName + "\"");
    }
}
