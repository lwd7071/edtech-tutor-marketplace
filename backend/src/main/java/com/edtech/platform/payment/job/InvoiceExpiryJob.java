package com.edtech.platform.payment.job;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InvoiceExpiryJob {
    private final InvoiceQueryRepository query;
    private final InvoiceCommandRepository command;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "InvoiceExpiryJob_expirePendingInvoices", lockAtLeastFor = "10s", lockAtMostFor = "5m")
    public void expirePendingInvoices() {
        var candidates = query.findPendingExpiredBefore(Instant.now(), PageRequest.of(0, 100));
        for (Invoice candidate : candidates) {
            transactions.executeWithoutResult(status -> command.findByIdForUpdate(candidate.getId()).ifPresent(invoice -> {
                if (invoice.getStatus() == com.edtech.platform.payment.domain.InvoiceStatus.PENDING
                        && invoice.getPaymentExpiredAt() != null
                        && invoice.getPaymentExpiredAt().isBefore(Instant.now())) invoice.expire();
            }));
        }
    }
}
