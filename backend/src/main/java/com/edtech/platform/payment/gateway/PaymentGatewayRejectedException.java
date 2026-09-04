package com.edtech.platform.payment.gateway;
public class PaymentGatewayRejectedException extends RuntimeException {
    public PaymentGatewayRejectedException(String message) { super(message); }
    public PaymentGatewayRejectedException(String message, Throwable cause) { super(message, cause); }
}
