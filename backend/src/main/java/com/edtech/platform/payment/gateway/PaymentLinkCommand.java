package com.edtech.platform.payment.gateway;
import java.net.URI;
public record PaymentLinkCommand(long orderCode, long amountVnd, String description, URI returnUrl, URI cancelUrl) {
    public PaymentLinkCommand {
        if (orderCode <= 0 || amountVnd <= 0) throw new IllegalArgumentException("payment values must be positive");
    }
}
