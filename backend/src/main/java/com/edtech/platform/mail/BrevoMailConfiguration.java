package com.edtech.platform.mail;

import com.edtech.platform.common.config.properties.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
class BrevoMailConfiguration {
    @Bean
    RestClient brevoRestClient(MailProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString().replaceAll("/+$", ""))
                .requestFactory(requestFactory)
                .defaultHeader("api-key", properties.apiKey())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
