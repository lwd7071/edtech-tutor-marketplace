package com.edtech.platform.payment.gateway;
public class InvalidPaymentSignatureException extends RuntimeException {
    public InvalidPaymentSignatureException() { super("Payment signature is invalid"); }
    public InvalidPaymentSignatureException(String message) { super(message); }
    public InvalidPaymentSignatureException(String message, Throwable cause) { super(message, cause); }
}
