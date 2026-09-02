package com.edtech.platform.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.stream.Stream;

@Component
@ConfigurationProperties(prefix = "app.payment")
@Getter @Setter
public class PaymentProviderProperties {
    private String provider = "disabled";
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String baseUrl = "https://api-merchant.payos.vn";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);

    @PostConstruct
    void validate() {
        if ("payos".equalsIgnoreCase(provider)
                && Stream.of(clientId, apiKey, checksumKey).anyMatch(v -> v == null || v.isBlank())) {
            throw new IllegalStateException("payOS provider requires client-id, api-key and checksum-key");
        }
    }
}
