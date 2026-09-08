package com.edtech.platform.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public record OAuthProperties(URI frontendRedirectUri) {
    public OAuthProperties {
        if (frontendRedirectUri == null || frontendRedirectUri.getHost() == null
                || !("http".equals(frontendRedirectUri.getScheme()) || "https".equals(frontendRedirectUri.getScheme()))
                || frontendRedirectUri.getQuery() != null || frontendRedirectUri.getFragment() != null) {
            throw new IllegalArgumentException("app.oauth2.frontend-redirect-uri must be an HTTP(S) URL without query or fragment");
        }
    }
}
