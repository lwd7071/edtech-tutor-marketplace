package com.edtech.platform.auth.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ConfigurationReporter {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ConfigurationReporter.class);

    private final String clientId;
    private final String clientSecret;

    public OAuth2ConfigurationReporter(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @PostConstruct
    void reportConfiguration() {
        if (clientId.isBlank() || clientSecret.isBlank()
                || clientId.startsWith("dummy-") || clientSecret.startsWith("dummy-")) {
            log.warn("Google OAuth is not configured; set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET");
        } else {
            log.info("Google OAuth configuration is ready");
        }
    }
}
