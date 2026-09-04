package com.edtech.platform.common;

import com.edtech.platform.ApiApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ApiApplication.class)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest extends AbstractPostgresContainerTest {

    private static final String REDIS_IMAGE = "redis:7-alpine";

    protected static GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379);
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.data.redis.url", () -> String.format("redis://%s:%d",
                REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(6379)));
    }
}
