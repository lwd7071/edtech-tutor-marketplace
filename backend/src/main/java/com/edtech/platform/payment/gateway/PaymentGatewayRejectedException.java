package com.edtech.platform.payment.gateway;
public class PaymentGatewayRejectedException extends RuntimeException {
    public PaymentGatewayRejectedException(String message) { super(message); }
}
