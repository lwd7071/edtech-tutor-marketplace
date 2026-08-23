package com.edtech.platform.auth.integration;

import com.edtech.platform.auth.service.EmailService;
import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(value = "isDockerAvailable", disabledReason = "Integration tests require Docker for Testcontainers")
public abstract class AuthIntegrationTestBase extends AbstractIntegrationTest {

    @MockBean
    protected EmailService emailService;

    @DynamicPropertySource
    static void configureAuthProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "2525");
    }
}
