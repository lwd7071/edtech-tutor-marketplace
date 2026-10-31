package com.edtech.platform.payment.gateway;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestProfilePaymentGatewayTest {
    private final TestProfilePaymentGateway gateway = new TestProfilePaymentGateway();

    @Test
    void createsOnlyNonRoutableTestCheckoutLinks() {
        PaymentLinkResult result = gateway.createPaymentLink(new PaymentLinkCommand(42, 100_000, "Gói học",
                URI.create("http://127.0.0.1:18085/student/payments/callback"),
                URI.create("http://127.0.0.1:18085/student/payments/callback")));

        assertThat(URI.create(result.checkoutUrl()).getHost()).isEqualTo("pay.test");
        assertThat(result.paymentLinkId()).isEqualTo("test-link-42");
        assertThat(result.qrCode()).startsWith("TEST-QR-");
        assertThat(result.expiresAt()).isNotNull();
        assertThat(gateway.findPaymentLink(42)).hasValueSatisfying(link ->
                assertThat(link.checkoutUrl()).isEqualTo(result.checkoutUrl()));
    }

    @Test
    void neverAcceptsFakeWebhookAsPayment() {
        assertThatThrownBy(() -> gateway.verifyWebhook(JsonNodeFactory.instance.objectNode()))
                .isInstanceOf(InvalidPaymentSignatureException.class);
    }
}
