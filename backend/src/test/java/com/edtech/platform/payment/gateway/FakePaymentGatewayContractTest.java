package com.edtech.platform.payment.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.assertj.core.api.Assertions.*;

class FakePaymentGatewayContractTest {
    private final PaymentLinkCommand command = new PaymentLinkCommand(10, 100_000, "Package", URI.create("https://app.test/return"), URI.create("https://app.test/cancel"));

    @Test
    void modelsCreateReconciliationAndProviderFailureScenarios() {
        FakePaymentGateway gateway = new FakePaymentGateway();
        assertThat(gateway.createPaymentLink(command).orderCode()).isEqualTo(10);
        assertThat(gateway.findPaymentLink(10)).isPresent();

        gateway = new FakePaymentGateway(); gateway.createMode = FakePaymentGateway.CreateMode.STORE_THEN_TIMEOUT;
        FakePaymentGateway storedThenTimeout = gateway;
        assertThatThrownBy(() -> storedThenTimeout.createPaymentLink(command)).isInstanceOf(PaymentGatewayUnavailableException.class);
        assertThat(storedThenTimeout.hasLink(10)).isTrue(); assertThat(storedThenTimeout.findPaymentLink(10)).isPresent();

        gateway = new FakePaymentGateway(); gateway.createMode = FakePaymentGateway.CreateMode.TIMEOUT_BEFORE_STORE;
        FakePaymentGateway noStore = gateway;
        assertThatThrownBy(() -> noStore.createPaymentLink(command)).isInstanceOf(PaymentGatewayUnavailableException.class);
        assertThat(noStore.findPaymentLink(10)).isEmpty();
        noStore.lookupMode = FakePaymentGateway.LookupMode.UNAVAILABLE;
        assertThatThrownBy(() -> noStore.findPaymentLink(10)).isInstanceOf(PaymentGatewayUnavailableException.class);

        gateway = new FakePaymentGateway(); gateway.lookupMode = FakePaymentGateway.LookupMode.EMPTY;
        assertThat(gateway.findPaymentLink(10)).isEmpty();
        gateway.lookupMode = FakePaymentGateway.LookupMode.UNAVAILABLE;
        FakePaymentGateway unavailable = gateway;
        assertThatThrownBy(() -> unavailable.findPaymentLink(10)).isInstanceOf(PaymentGatewayUnavailableException.class);
        gateway.createMode = FakePaymentGateway.CreateMode.REJECT;
        FakePaymentGateway rejected = gateway;
        assertThatThrownBy(() -> rejected.createPaymentLink(command)).isInstanceOf(PaymentGatewayRejectedException.class);
    }

    @Test
    void modelsWebhookSignatureReplayAndMismatchScenarios() {
        ObjectMapper mapper = new ObjectMapper();
        FakePaymentGateway gateway = new FakePaymentGateway();
        gateway.createPaymentLink(command);
        var payload = mapper.createObjectNode().put("reference", "ref-1").put("orderCode", 10).put("amount", 100_000);
        assertThat(gateway.verifyWebhook(payload).providerReference()).isEqualTo("ref-1");
        gateway.verifyWebhook(payload);
        assertThat(gateway.hasProcessed("ref-1")).isTrue();
        gateway.signatureValid = false;
        assertThatThrownBy(() -> gateway.verifyWebhook(payload)).isInstanceOf(InvalidPaymentSignatureException.class);
        gateway.signatureValid = true;
        assertThatThrownBy(() -> gateway.verifyWebhook(mapper.createObjectNode().put("reference", "wrong-amount").put("orderCode", 10).put("amount", 99_999)))
                .isInstanceOf(PaymentGatewayRejectedException.class);
        assertThatThrownBy(() -> gateway.verifyWebhook(mapper.createObjectNode().put("reference", "bad").put("orderCode", 0).put("amount", 0)))
                .isInstanceOf(PaymentGatewayRejectedException.class);
    }
}
