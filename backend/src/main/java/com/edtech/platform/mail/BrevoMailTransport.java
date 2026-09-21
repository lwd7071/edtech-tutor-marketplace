package com.edtech.platform.mail;

import com.edtech.platform.common.config.properties.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
public class BrevoMailTransport implements MailTransport {
    private static final String DUPLICATE_IDEMPOTENCY_ERROR = "duplicate_parameter";

    private final RestClient client;
    private final MailProperties properties;

    public BrevoMailTransport(RestClient brevoRestClient, MailProperties properties) {
        this.client = brevoRestClient;
        this.properties = properties;
    }

    @Override
    public void send(OutboundMail mail) {
        try {
            client.post()
                    .uri("/v3/smtp/email")
                    .body(new BrevoEmailRequest(
                            new EmailAddress(properties.from(), properties.senderName()),
                            List.of(new EmailAddress(mail.recipient())),
                            mail.subject(),
                            mail.body(),
                            Map.of("idempotencyKey", mail.id().toString())))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException failure) {
            if (isAlreadyAccepted(failure)) {
                return;
            }
            if (isRetryable(failure)) {
                throw new IllegalStateException("Brevo temporarily unavailable", failure);
            }
            throw new PermanentMailDeliveryException("Brevo rejected the email request");
        }
    }

    private boolean isAlreadyAccepted(RestClientResponseException failure) {
        return failure.getResponseBodyAsString().contains(DUPLICATE_IDEMPOTENCY_ERROR);
    }

    private boolean isRetryable(RestClientResponseException failure) {
        int status = failure.getStatusCode().value();
        return status == HttpStatus.TOO_MANY_REQUESTS.value() || status >= 500;
    }

    private record BrevoEmailRequest(
            EmailAddress sender,
            List<EmailAddress> to,
            String subject,
            String textContent,
            Map<String, String> headers) {
    }

    private record EmailAddress(String email, String name) {
        private EmailAddress(String email) {
            this(email, null);
        }
    }
}
