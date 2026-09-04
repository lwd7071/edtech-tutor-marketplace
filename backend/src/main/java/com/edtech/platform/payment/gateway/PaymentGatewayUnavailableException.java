package com.edtech.platform.payment.gateway;
public class PaymentGatewayUnavailableException extends RuntimeException {
    public PaymentGatewayUnavailableException(String message) { super(message); }
    public PaymentGatewayUnavailableException(String message, Throwable cause) { super(message, cause); }
}
