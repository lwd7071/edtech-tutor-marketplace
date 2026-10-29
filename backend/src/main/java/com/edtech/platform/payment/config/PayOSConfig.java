package com.edtech.platform.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

@Configuration
@Conditional(OnPayOsProviderCondition.class)
public class PayOSConfig {

    @Bean
    public PayOS payOS(PaymentProviderProperties properties) {
        int timeoutMs = Math.toIntExact(Math.max(properties.getConnectTimeout().toMillis(), properties.getReadTimeout().toMillis()));
        ClientOptions options = ClientOptions.builder()
                .clientId(properties.getClientId())
                .apiKey(properties.getApiKey())
                .checksumKey(properties.getChecksumKey())
                .baseURL(properties.getBaseUrl())
                .timeoutMs(timeoutMs)
                .maxRetries(0)
                .build();
        return new PayOS(options);
    }
}
