package com.edtech.platform.common;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String REDIS_IMAGE = "redis:7-alpine";

    protected static PostgreSQLContainer<?> POSTGRES_CONTAINER;
    protected static GenericContainer<?> REDIS_CONTAINER;

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    static {
        if (isDockerAvailable()) {
            POSTGRES_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("edtech_test_db")
                    .withUsername("edtech_test_user")
                    .withPassword("edtech_test_password")
                    .withReuse(true);
            POSTGRES_CONTAINER.start();

            REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
                    .withExposedPorts(6379)
                    .withReuse(true);
            REDIS_CONTAINER.start();
        }
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        if (isDockerAvailable() && POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

            registry.add("spring.data.redis.url", () -> String.format("redis://%s:%d",
                    REDIS_CONTAINER.getHost(),
                    REDIS_CONTAINER.getMappedPort(6379)));
        } else {
            // Fallback for environments without Docker (e.g. disable flyway during lightweight context checks or use mock)
            registry.add("spring.flyway.enabled", () -> "false");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/edtech_db");
            registry.add("spring.datasource.username", () -> "edtech_user");
            registry.add("spring.datasource.password", () -> "edtech_password");
            registry.add("spring.data.redis.url", () -> "redis://localhost:6379");
        }
    }
}
