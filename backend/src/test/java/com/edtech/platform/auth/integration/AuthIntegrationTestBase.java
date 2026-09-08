package com.edtech.platform.auth.integration;

import com.edtech.platform.mail.MailService;
import com.edtech.platform.common.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AuthIntegrationTestBase extends AbstractIntegrationTest {

    @MockBean
    protected MailService emailService;

    @DynamicPropertySource
    static void configureAuthProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "2525");
    }
}
