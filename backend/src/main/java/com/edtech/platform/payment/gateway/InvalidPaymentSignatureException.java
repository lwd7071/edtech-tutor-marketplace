package com.edtech.platform.payment.gateway;
public class InvalidPaymentSignatureException extends RuntimeException {
    public InvalidPaymentSignatureException() { super("Payment signature is invalid"); }
}
