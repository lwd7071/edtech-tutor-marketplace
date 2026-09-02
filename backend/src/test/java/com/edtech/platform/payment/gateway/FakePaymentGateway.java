package com.edtech.platform.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FakePaymentGateway implements PaymentGateway {
    enum CreateMode { SUCCESS, STORE_THEN_TIMEOUT, TIMEOUT_BEFORE_STORE, REJECT }
    enum LookupMode { NORMAL, EMPTY, UNAVAILABLE }
    private final Map<Long, PaymentLinkStatus> links = new HashMap<>();
    private final Map<Long, Long> expectedAmounts = new HashMap<>();
    private final Set<String> processedReferences = new HashSet<>();
    CreateMode createMode = CreateMode.SUCCESS;
    LookupMode lookupMode = LookupMode.NORMAL;
    boolean signatureValid = true;

    public PaymentLinkResult createPaymentLink(PaymentLinkCommand command) {
        if (createMode == CreateMode.REJECT) throw new PaymentGatewayRejectedException("rejected");
        if (createMode == CreateMode.TIMEOUT_BEFORE_STORE) throw new PaymentGatewayUnavailableException("timeout");
        PaymentLinkStatus status = new PaymentLinkStatus(command.orderCode(), "link-" + command.orderCode(),
                "https://pay.test/" + command.orderCode(), "qr", Instant.parse("2026-08-27T01:00:00Z"), "PENDING");
        links.put(command.orderCode(), status);
        expectedAmounts.put(command.orderCode(), command.amountVnd());
        if (createMode == CreateMode.STORE_THEN_TIMEOUT) throw new PaymentGatewayUnavailableException("response lost");
        return new PaymentLinkResult(status.orderCode(), status.paymentLinkId(), status.checkoutUrl(), status.qrCode(), status.expiresAt());
    }
    public Optional<PaymentLinkStatus> findPaymentLink(long orderCode) {
        if (lookupMode == LookupMode.UNAVAILABLE) throw new PaymentGatewayUnavailableException("lookup unavailable");
        if (lookupMode == LookupMode.EMPTY) return Optional.empty();
        return Optional.ofNullable(links.get(orderCode));
    }
    public VerifiedPayment verifyWebhook(JsonNode payload) {
        if (!signatureValid) throw new InvalidPaymentSignatureException();
        long orderCode = payload.path("orderCode").asLong();
        long amount = payload.path("amount").asLong();
        if (orderCode <= 0 || amount <= 0) throw new PaymentGatewayRejectedException("webhook amount/order mismatch");
        if (expectedAmounts.containsKey(orderCode) && expectedAmounts.get(orderCode) != amount) {
            throw new PaymentGatewayRejectedException("webhook amount/order mismatch");
        }
        String reference = payload.path("reference").asText();
        processedReferences.add(reference);
        return new VerifiedPayment(reference, orderCode, amount, Instant.parse("2026-08-27T00:00:00Z"), payload.deepCopy());
    }
    boolean hasLink(long orderCode) { return links.containsKey(orderCode); }
    boolean hasProcessed(String reference) { return processedReferences.contains(reference); }
}
