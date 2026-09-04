package com.edtech.platform.payment.dto;
import com.edtech.platform.payment.domain.Invoice;
import java.time.Instant;
import java.util.UUID;
public record StudentInvoiceDetail(UUID id, String invoiceNumber, String status, long amountVnd,
                                   long payosOrderCode, String checkoutUrl, String qrCode, Instant paymentExpiredAt, Instant paidAt) {
    public static StudentInvoiceDetail from(Invoice i) { return new StudentInvoiceDetail(i.getId(), i.getInvoiceNumber(), i.getStatus().name(), i.getAmountVnd(), i.getPayosOrderCode(), i.getCheckoutUrl(), i.getQrCode(), i.getPaymentExpiredAt(), i.getPaidAt()); }
}
