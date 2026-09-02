package com.edtech.platform.payment.gateway;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
public interface PaymentGateway {
    PaymentLinkResult createPaymentLink(PaymentLinkCommand command);
    Optional<PaymentLinkStatus> findPaymentLink(long orderCode);
    VerifiedPayment verifyWebhook(JsonNode payload);
}
