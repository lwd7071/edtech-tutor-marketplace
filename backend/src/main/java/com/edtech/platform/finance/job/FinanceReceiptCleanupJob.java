package com.edtech.platform.finance.job;

import com.edtech.platform.finance.repository.FinanceCommandReceiptRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class FinanceReceiptCleanupJob {
    private final FinanceCommandReceiptRepository receipts;
    private final Clock clock;

    @Scheduled(cron = "0 20 3 * * *")
    @SchedulerLock(name = "financeCommandReceiptCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1S")
    public void deleteExpired() {
        receipts.deleteExpired(clock.instant());
    }
}
