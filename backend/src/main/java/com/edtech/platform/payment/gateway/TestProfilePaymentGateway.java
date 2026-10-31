package com.edtech.platform.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Optional;

@Service
@Profile("test")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "fake")
public class TestProfilePaymentGateway implements PaymentGateway {
    private final ConcurrentMap<Long, PaymentLinkStatus> links = new ConcurrentHashMap<>();

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkCommand command) {
        PaymentLinkStatus link = links.computeIfAbsent(command.orderCode(), orderCode -> new PaymentLinkStatus(
                orderCode, "test-link-" + orderCode, "https://pay.test/checkout/" + orderCode,
                "TEST-QR-" + orderCode, Instant.now().plus(15, ChronoUnit.MINUTES), "PENDING"));
        return new PaymentLinkResult(link.orderCode(), link.paymentLinkId(), link.checkoutUrl(), link.qrCode(), link.expiresAt());
    }

    @Override
    public Optional<PaymentLinkStatus> findPaymentLink(long orderCode) {
        return Optional.ofNullable(links.get(orderCode));
    }

    @Override
    public VerifiedPayment verifyWebhook(JsonNode payload) {
        throw new InvalidPaymentSignatureException("Test provider does not accept webhooks");
    }
}
