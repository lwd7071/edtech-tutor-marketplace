package com.edtech.platform.auth.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ConfigurationReporter {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ConfigurationReporter.class);

    private final OAuth2ClientProperties.Registration google;

    public OAuth2ConfigurationReporter(OAuth2ClientProperties properties) {
        this.google = properties.getRegistration().get("google");
    }

    @PostConstruct
    void reportConfiguration() {
        String clientId = google == null ? null : google.getClientId();
        String clientSecret = google == null ? null : google.getClientSecret();
        if (clientId == null || clientSecret == null || clientId.isBlank() || clientSecret.isBlank()
                || clientId.startsWith("dummy-") || clientSecret.startsWith("dummy-")) {
            log.warn("Google OAuth is not configured; set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET");
        } else {
            log.info("Google OAuth configuration is ready");
        }
    }
}
