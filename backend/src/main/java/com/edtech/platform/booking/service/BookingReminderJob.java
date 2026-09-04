package com.edtech.platform.booking.service;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingReminderJob {

    private final BookingRepository repo;
    private final CommunicationFacade communicationFacade;
    private final PlatformSettingsFacade platformSettingsFacade;

    @Scheduled(fixedDelay = 900000)
    @SchedulerLock(name = "BookingReminderJob_publish", lockAtMostFor = "5m", lockAtLeastFor = "10s")
    public void publish() {
        try {
            int reminderHours = 11;
            Instant now = Instant.now();
            Instant from = now.plus(Duration.ofHours(reminderHours - 1));
            Instant to = now.plus(Duration.ofHours(reminderHours + 1));
            repo.findReminderCandidates(from, to, PageRequest.of(0, 100)).forEach(b -> {
                try {
                    communicationFacade.sendBookingReminder(b.getTeacherId(), b.getId());
                    communicationFacade.sendBookingReminder(b.getStudentId(), b.getId());
                } catch (Exception ex) {
                    log.error("Failed to send booking reminder for booking {}", b.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error executing BookingReminderJob", e);
        }
    }
}