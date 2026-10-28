package com.edtech.platform.payment.job;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.edtech.platform.payment.gateway.PaymentLinkStatus;
import com.edtech.platform.payment.gateway.VerifiedPayment;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import com.edtech.platform.payment.service.PaymentWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvoiceReconciliationJobTest {
    @Test
    void continuesAcrossShrinkingPagesAndRequiresProviderEvidence() {
        InvoiceQueryRepository query = mock(InvoiceQueryRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentWebhookService webhook = mock(PaymentWebhookService.class);
        List<Invoice> first = java.util.stream.LongStream.rangeClosed(1, 50).mapToObj(n -> invoice(n)).toList();
        Invoice last = invoice(51);
        when(query.findPendingExpiredAfterOrderCode(any(), eq(0L), eq(50))).thenReturn(first);
        when(query.findPendingExpiredAfterOrderCode(any(), eq(50L), eq(50))).thenReturn(List.of(last));
        when(gateway.findPaymentLink(anyLong())).thenAnswer(call -> {
            long code = call.getArgument(0);
            VerifiedPayment evidence = code == 51 ? new VerifiedPayment("REF-51", 51, 100, Instant.parse("2026-01-01T00:00:00Z"), null) : null;
            return Optional.of(new PaymentLinkStatus(code, "link", "url", null, null, "PAID", evidence));
        });
        new InvoiceReconciliationJob(query, mock(InvoiceCommandRepository.class), gateway, webhook, mock(TransactionTemplate.class))
                .reconcilePendingInvoices();
        verify(webhook, times(1)).processWebhook(argThat(p -> p.orderCode() == 51 && p.amountVnd() == 100));
    }

    private static Invoice invoice(long code) {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getPayosOrderCode()).thenReturn(code);
        lenient().when(invoice.getAmountVnd()).thenReturn(100L);
        lenient().when(invoice.getInvoiceNumber()).thenReturn("INV-" + code);
        return invoice;
    }
}
