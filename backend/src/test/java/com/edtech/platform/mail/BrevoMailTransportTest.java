package com.edtech.platform.mail;

import com.edtech.platform.common.config.properties.MailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrevoMailTransportTest {
    private MockRestServiceServer server;
    private BrevoMailTransport transport;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.brevo.test")
                .defaultHeader("api-key", "xkeysib-test-key")
                .defaultHeader("Accept", "application/json");
        server = MockRestServiceServer.bindTo(builder).build();
        transport = new BrevoMailTransport(builder.build(), properties());
    }

    @Test
    void sendsTextEmailWithAuthenticationAndStableOutboxIdempotencyKey() {
        UUID id = UUID.randomUUID();
        server.expect(once(), requestTo("https://api.brevo.test/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test-key"))
                .andExpect(header("Accept", "application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "sender": {"email": "verified.sender@gmail.com", "name": "MinBack"},
                          "to": [{"email": "student@example.test"}],
                          "subject": "Verify",
                          "textContent": "Token link",
                          "headers": {"idempotencyKey": "%s"}
                        }
                        """.formatted(id)))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"brevo-message-id\"}"));

        assertDoesNotThrow(() -> transport.send(
                new OutboundMail(id, "student@example.test", "Verify", "Token link", 0)));
        server.verify();
    }

    @Test
    void rateLimitIsRetryable() {
        server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(IllegalStateException.class, () -> transport.send(mail()));
    }

    @Test
    void validationFailureIsPermanent() {
        server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"invalid_parameter\"}"));

        assertThrows(PermanentMailDeliveryException.class, () -> transport.send(mail()));
    }

    @Test
    void duplicateIdempotencyResponseCountsAsAlreadyAccepted() {
        server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"duplicate_parameter\"}"));

        assertDoesNotThrow(() -> transport.send(mail()));
    }

    private static OutboundMail mail() {
        return new OutboundMail(UUID.randomUUID(), "student@example.test", "Verify", "Token link", 0);
    }

    private static MailProperties properties() {
        return new MailProperties(
                "brevo",
                "verified.sender@gmail.com",
                "MinBack",
                URI.create("http://localhost:3000"),
                10_000,
                "xkeysib-test-key",
                URI.create("https://api.brevo.test"),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5));
    }
}
