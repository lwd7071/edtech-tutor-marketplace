package com.edtech.platform.payment.gateway;
public class PaymentGatewayUnavailableException extends RuntimeException {
    public PaymentGatewayUnavailableException(String message) { super(message); }
}
