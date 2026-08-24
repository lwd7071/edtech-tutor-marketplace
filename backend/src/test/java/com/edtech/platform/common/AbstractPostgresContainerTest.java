package com.edtech.platform.common;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractPostgresContainerTest {
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("edtech_test_db")
                .withUsername("edtech_test_user")
                .withPassword("edtech_test_password");
        POSTGRES_CONTAINER.start();
    }
}
