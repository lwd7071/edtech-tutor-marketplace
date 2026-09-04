package com.edtech.platform.booking.service;

import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageExpiryJob {

    private final EnrollmentBookingFacade enrollmentBookingFacade;

    @Scheduled(fixedDelay = 600000)
    @SchedulerLock(name = "PackageExpiryJob_lockExpired", lockAtMostFor = "10m", lockAtLeastFor = "10s")
    public void lockExpired() {
        try {
            enrollmentBookingFacade.lockExpiredPackages(Instant.now(), PageRequest.of(0, 100));
        } catch (Exception e) {
            log.error("Error executing PackageExpiryJob", e);
        }
    }
}