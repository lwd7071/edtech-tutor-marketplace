package com.edtech.platform.common.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "app.email")
public record MailProperties(
        @NotBlank String provider,
        @NotBlank @Email String from,
        URI frontendUrl,
        @Positive long pollDelayMs
) {
    public MailProperties {
        if (frontendUrl == null || frontendUrl.getHost() == null
                || !("http".equals(frontendUrl.getScheme()) || "https".equals(frontendUrl.getScheme()))
                || frontendUrl.getQuery() != null || frontendUrl.getFragment() != null) {
            throw new IllegalArgumentException("app.email.frontend-url must be an HTTP(S) URL without query or fragment");
        }
    }

    public String normalizedFrontendUrl() {
        return frontendUrl.toString().replaceAll("/+$", "");
    }
}
