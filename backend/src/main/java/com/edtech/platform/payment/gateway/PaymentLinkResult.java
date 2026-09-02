package com.edtech.platform.payment.gateway;
import java.time.Instant;
public record PaymentLinkResult(long orderCode, String paymentLinkId, String checkoutUrl, String qrCode, Instant expiresAt) { }
