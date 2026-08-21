package com.edtech.platform.ranking.service;

import com.edtech.platform.common.event.booking.BookingCompletedEvent;
import com.edtech.platform.ranking.domain.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherStatsEventListener {

    private final TeacherStatsService teacherStatsService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        log.info("Received ReviewCreatedEvent for teacher: {}", event.getTeacherId());
        teacherStatsService.recalculateTeacherStats(event.getTeacherId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.info("Received BookingCompletedEvent for teacher: {}", event.getTeacherId());
        teacherStatsService.recalculateTeacherStats(event.getTeacherId());
    }
}
