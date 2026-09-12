package com.edtech.platform.payment.dto;

import com.edtech.platform.payment.domain.Invoice;
import java.time.Instant;
import java.util.UUID;

public record InvoiceDetail(
        UUID id,
        String invoiceNumber,
        UUID pricingPackageId,
        long amountVnd,
        String status,
        String checkoutUrl,
        String qrCode,
        Instant paymentExpiredAt,
        Instant paidAt
) {
    public static InvoiceDetail from(Invoice invoice) {
        return new InvoiceDetail(
                invoice.getId(), invoice.getInvoiceNumber(), invoice.getPricingPackageId(),
                invoice.getAmountVnd(), invoice.getStatus().name(), invoice.getCheckoutUrl(),
                invoice.getQrCode(), invoice.getPaymentExpiredAt(), invoice.getPaidAt());
    }
}
