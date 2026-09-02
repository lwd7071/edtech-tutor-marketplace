package com.edtech.platform.payment.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {
    private Invoice pending() {
        return Invoice.pending("INV-20260827-1", 1L, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 500_000L, UUID.randomUUID(), "a".repeat(64));
    }

    @Test
    void pendingInvoiceTransitionsOnceAndPaymentLinkCannotBeOverwritten() {
        Invoice invoice = pending();
        invoice.attachPaymentLink("link-1", "https://pay.example/1", "qr", Instant.now().plusSeconds(600));
        invoice.attachPaymentLink("link-1", "https://pay.example/1", "qr", invoice.getPaymentExpiredAt());
        assertThatThrownBy(() -> invoice.attachPaymentLink("link-2", "https://pay.example/2", "qr2", Instant.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_INVALID_STATE);
        invoice.markPaid(Instant.now());
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThatThrownBy(invoice::cancel).isInstanceOf(BusinessException.class);
    }

    @Test
    void pendingFactoryRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> Invoice.pending("INV", 1L, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 0, UUID.randomUUID(), "a".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
