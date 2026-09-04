package com.edtech.platform.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledPaymentGateway implements PaymentGateway {

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkCommand command) {
        throw new PaymentGatewayUnavailableException("Payment gateway is disabled");
    }

    @Override
    public Optional<PaymentLinkStatus> findPaymentLink(long orderCode) {
        throw new PaymentGatewayUnavailableException("Payment gateway is disabled");
    }

    @Override
    public VerifiedPayment verifyWebhook(JsonNode payload) {
        throw new PaymentGatewayUnavailableException("Payment gateway is disabled");
    }
}
