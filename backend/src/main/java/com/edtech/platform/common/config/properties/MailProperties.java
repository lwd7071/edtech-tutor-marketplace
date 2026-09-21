package com.edtech.platform.common.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.email")
public record MailProperties(
        @NotBlank String provider,
        @NotBlank @Email String from,
        @NotBlank String senderName,
        URI frontendUrl,
        @Positive long pollDelayMs,
        String apiKey,
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
    public MailProperties {
        if (frontendUrl == null || frontendUrl.getHost() == null
                || !("http".equals(frontendUrl.getScheme()) || "https".equals(frontendUrl.getScheme()))
                || frontendUrl.getQuery() != null || frontendUrl.getFragment() != null) {
            throw new IllegalArgumentException("app.email.frontend-url must be an HTTP(S) URL without query or fragment");
        }
        if ("brevo".equalsIgnoreCase(provider)) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("BREVO_API_KEY is required when app.email.provider=brevo");
            }
            validateHttpUrl(baseUrl, "app.email.base-url");
            if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
                throw new IllegalArgumentException("app.email.connect-timeout must be positive");
            }
            if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
                throw new IllegalArgumentException("app.email.read-timeout must be positive");
            }
        }
    }

    public String normalizedFrontendUrl() {
        return frontendUrl.toString().replaceAll("/+$", "");
    }

    private static void validateHttpUrl(URI uri, String propertyName) {
        if (uri == null || uri.getHost() == null
                || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(propertyName + " must be an HTTP(S) URL without query or fragment");
        }
    }
}
