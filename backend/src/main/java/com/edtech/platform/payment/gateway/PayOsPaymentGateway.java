package com.edtech.platform.payment.gateway;

import com.edtech.platform.payment.config.PaymentProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import com.edtech.platform.payment.config.OnPayOsProviderCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.NotFoundException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.WebhookData;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@Conditional(OnPayOsProviderCondition.class)
public class PayOsPaymentGateway implements PaymentGateway {

    private final PayOS payOS;
    private final PaymentProviderProperties properties;

    public PayOsPaymentGateway(PayOS payOS, PaymentProviderProperties properties) {
        this.payOS = payOS;
        this.properties = properties;
    }

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkCommand command) {
        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(command.orderCode())
                .amount(command.amountVnd())
                .description(command.description())
                .returnUrl(command.returnUrl().toString())
                .cancelUrl(command.cancelUrl().toString())
                .build();

        try {
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);
            if (response == null || response.getOrderCode() != command.orderCode()
                    || response.getPaymentLinkId() == null || response.getPaymentLinkId().isBlank()
                    || response.getCheckoutUrl() == null || response.getCheckoutUrl().isBlank()) {
                throw new PaymentGatewayRejectedException("PayOS returned an invalid payment link response");
            }
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
            return new PaymentLinkResult(
                    response.getOrderCode(),
                    response.getPaymentLinkId(),
                    response.getCheckoutUrl(),
                    response.getQrCode(),
                    expiresAt
            );
        } catch (PaymentGatewayRejectedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link for orderCode={}", command.orderCode(), e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("rejected")) {
                throw new PaymentGatewayRejectedException("PayOS rejected payment link request: " + e.getMessage(), e);
            }
            throw new PaymentGatewayUnavailableException("Failed to create PayOS payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<PaymentLinkStatus> findPaymentLink(long orderCode) {
        try {
            PaymentLink info = payOS.paymentRequests().get(orderCode);
            if (info == null) {
                return Optional.empty();
            }
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
            String checkoutUrl = info.getId() != null ? "https://checkout.payos.vn/web/" + info.getId() : null;
            VerifiedPayment payment = null;
            if (info.getOrderCode() != null && info.getOrderCode() == orderCode && info.getAmount() != null
                    && info.getAmountPaid() != null && info.getAmountRemaining() != null
                    && info.getAmount().equals(info.getAmountPaid()) && info.getAmountRemaining() == 0
                    && info.getTransactions() != null && info.getTransactions().size() == 1) {
                var tx = info.getTransactions().get(0);
                if (tx.getReference() != null && !tx.getReference().isBlank() && tx.getAmount() != null
                        && tx.getAmount().equals(info.getAmountPaid()) && tx.getTransactionDateTime() != null) {
                    try {
                        Instant paidAt = LocalDateTime.parse(tx.getTransactionDateTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
                        payment = new VerifiedPayment(tx.getReference(), orderCode, tx.getAmount(), paidAt, null);
                    } catch (java.time.DateTimeException ignored) {
                        log.warn("Cannot parse provider transaction time for orderCode={}", orderCode);
                    }
                }
            }
            return Optional.of(new PaymentLinkStatus(
                    info.getOrderCode(),
                    info.getId(),
                    checkoutUrl,
                    null,
                    expiresAt,
                    info.getStatus() != null ? info.getStatus().name() : "UNKNOWN",
                    payment
            ));
        } catch (NotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("not found") || msg.contains("404") || msg.contains("không tìm thấy")) {
                return Optional.empty();
            }
            log.error("Failed to query PayOS payment link for orderCode={}", orderCode, e);
            throw new PaymentGatewayUnavailableException("Failed to query PayOS payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public VerifiedPayment verifyWebhook(JsonNode payload) {
        try {
            WebhookData webhookData = payOS.webhooks().verify(payload);
            if (webhookData == null) {
                throw new InvalidPaymentSignatureException("PayOS webhook verification returned null");
            }
            Instant paidAt = Instant.now();
            if (webhookData.getTransactionDateTime() != null && !webhookData.getTransactionDateTime().isBlank()) {
                try {
                    paidAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(webhookData.getTransactionDateTime()).toInstant();
                } catch (Exception parseEx) {
                    paidAt = Instant.now();
                }
            }
            String reference = webhookData.getReference();
            if (reference == null || reference.isBlank() || webhookData.getOrderCode() <= 0
                    || webhookData.getAmount() <= 0 || (webhookData.getCode() != null && !"00".equals(webhookData.getCode()))) {
                throw new PaymentGatewayRejectedException("PayOS webhook is missing payment identity or amount");
            }
            ObjectNode sanitized = JsonNodeFactory.instance.objectNode();
            sanitized.put("orderCode", webhookData.getOrderCode());
            sanitized.put("amount", webhookData.getAmount());
            sanitized.put("reference", reference);
            if (webhookData.getPaymentLinkId() != null) sanitized.put("paymentLinkId", webhookData.getPaymentLinkId());
            return new VerifiedPayment(
                    reference,
                    webhookData.getOrderCode(),
                    webhookData.getAmount(),
                    paidAt,
                    sanitized
            );
        } catch (InvalidPaymentSignatureException e) {
            throw e;
        } catch (PaymentGatewayRejectedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify PayOS webhook signature/payload", e);
            throw new InvalidPaymentSignatureException("Invalid PayOS webhook signature: " + e.getMessage(), e);
        }
    }
}
