package com.edtech.platform.booking.service;

import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingExpiryJob {

    private final BookingRepository repo;
    private final EnrollmentBookingFacade packages;
    private final TransactionTemplate tx;

    @Scheduled(fixedDelay = 300000)
    @SchedulerLock(name = "BookingExpiryJob_expire", lockAtMostFor = "5m", lockAtLeastFor = "10s")
    public void expire() {
        try {
            Instant cutoff = Instant.now().minus(Duration.ofHours(12));
            repo.findExpiryCandidates(cutoff, PageRequest.of(0, 100)).forEach(c ->
                tx.executeWithoutResult(status ->
                    repo.findByIdForUpdate(c.getId()).ifPresent(b -> {
                        if (b.getStatus() == BookingStatus.SCHEDULED) {
                            if (!b.isTrial()) {
                                packages.releaseReservedSession(b.getStudentPackageId());
                            }
                            b.expire(Instant.now());
                        }
                    })
                )
            );
        } catch (Exception e) {
            log.error("Error executing BookingExpiryJob", e);
        }
    }
}