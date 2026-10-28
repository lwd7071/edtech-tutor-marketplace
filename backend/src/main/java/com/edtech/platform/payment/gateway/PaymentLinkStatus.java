package com.edtech.platform.payment.gateway;
import java.time.Instant;
public record PaymentLinkStatus(long orderCode, String paymentLinkId, String checkoutUrl, String qrCode, Instant expiresAt, String status, VerifiedPayment verifiedPayment) {
    public PaymentLinkStatus(long orderCode, String paymentLinkId, String checkoutUrl, String qrCode, Instant expiresAt, String status) {
        this(orderCode, paymentLinkId, checkoutUrl, qrCode, expiresAt, status, null);
    }
}
