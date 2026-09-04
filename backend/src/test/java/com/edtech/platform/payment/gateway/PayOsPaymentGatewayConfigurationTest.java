package com.edtech.platform.payment.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

import static org.assertj.core.api.Assertions.assertThat;

class PayOsPaymentGatewayConfigurationTest {

    @Configuration
    @EnableConfigurationProperties(com.edtech.platform.payment.config.PaymentProviderProperties.class)
    static class Config {}

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    Config.class,
                    com.edtech.platform.payment.config.PayOSConfig.class,
                    PayOsPaymentGateway.class);

    @Test
    void shouldNotLoadBeans_whenProviderIsNotPayos() {
        contextRunner.withPropertyValues("app.payment.provider=disabled")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PayOS.class);
                    assertThat(context).doesNotHaveBean(PayOsPaymentGateway.class);
                });
    }

    @Test
    void shouldLoadBeans_whenProviderIsPayos_AndCredentialsAreProvided() {
        contextRunner.withPropertyValues(
                "app.payment.provider=payos",
                "app.payment.client-id=client123",
                "app.payment.api-key=api123",
                "app.payment.checksum-key=checksum123"
        ).run(context -> {
            assertThat(context).hasSingleBean(PayOS.class);
            assertThat(context).hasSingleBean(PayOsPaymentGateway.class);
        });
    }

    @Test
    void shouldFailToStart_whenProviderIsPayos_ButMissingCredentials() {
        contextRunner.withPropertyValues(
                "app.payment.provider=payos"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .getRootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("payOS provider requires client-id, api-key and checksum-key");
        });
    }
}
