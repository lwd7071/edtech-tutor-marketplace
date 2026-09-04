package com.edtech.platform.payment.job;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.edtech.platform.payment.gateway.PaymentLinkStatus;
import com.edtech.platform.payment.gateway.VerifiedPayment;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import com.edtech.platform.payment.service.PaymentWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceReconciliationJob {

    private final InvoiceQueryRepository invoiceQueryRepository;
    private final InvoiceCommandRepository invoiceCommandRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentWebhookService paymentWebhookService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Run every 10 minutes (600,000 ms)
    @Scheduled(fixedDelay = 600000)
    @SchedulerLock(name = "InvoiceReconciliationJob_reconcilePendingInvoices", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    public void reconcilePendingInvoices() {
        log.info("Starting Invoice Reconciliation Job...");

        // Find invoices that were created at least 15 minutes ago
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);

        int page = 0;
        int size = 50;
        List<Invoice> pendingInvoices;

        do {
            pendingInvoices = invoiceQueryRepository.findPendingExpiredBefore(cutoff, PageRequest.of(page, size));

            for (Invoice invoice : pendingInvoices) {
                try {
                    processReconciliation(invoice);
                } catch (Exception e) {
                    log.error("Error reconciling invoice: {}", invoice.getInvoiceNumber(), e);
                }
            }
            page++;
        } while (pendingInvoices.size() == size);

        log.info("Finished Invoice Reconciliation Job.");
    }

    private void processReconciliation(Invoice invoice) {
        log.info("Reconciling invoice: {} (orderCode: {})", invoice.getInvoiceNumber(), invoice.getPayosOrderCode());

        Optional<PaymentLinkStatus> linkStatusOpt = paymentGateway.findPaymentLink(invoice.getPayosOrderCode());

        if (linkStatusOpt.isEmpty()) {
            // Transaction doesn't exist on PayOS
            cancelInvoice(invoice.getId());
            return;
        }

        PaymentLinkStatus linkStatus = linkStatusOpt.get();
        String status = linkStatus.status();

        if ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            VerifiedPayment verifiedPayment = new VerifiedPayment(
                    "RECONCILE-" + invoice.getInvoiceNumber(),
                    invoice.getPayosOrderCode(),
                    invoice.getAmountVnd(), // Assuming fully paid
                    Instant.now(),
                    null // Dummy sanitized payload
            );

            try {
                paymentWebhookService.processWebhook(verifiedPayment);
                log.info("Successfully recovered paid invoice: {}", invoice.getInvoiceNumber());
            } catch (Exception e) {
                log.error("Failed to process recovered invoice: {}", invoice.getInvoiceNumber(), e);
            }
        } else if ("CANCELLED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
            cancelInvoice(invoice.getId());
        } else {
            log.info("Invoice {} is still pending on gateway (status: {}). Ignoring.", invoice.getInvoiceNumber(), status);
        }
    }

    private void cancelInvoice(UUID invoiceId) {
        try {
            transactionTemplate.execute(status -> {
                invoiceCommandRepository.findByIdForUpdate(invoiceId).ifPresent(Invoice::cancel);
                return null;
            });
            log.info("Cancelled invoice {}", invoiceId);
        } catch (Exception e) {
            log.error("Failed to cancel invoice {}", invoiceId, e);
        }
    }
}
