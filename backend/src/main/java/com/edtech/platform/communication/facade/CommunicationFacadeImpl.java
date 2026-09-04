package com.edtech.platform.communication.facade;

import com.edtech.platform.booking.facade.BookingEvent;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.communication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunicationFacadeImpl implements CommunicationFacade {

    private final NotificationService notifications;
    private final ApplicationEventPublisher publisher;

    @Override
    public void publishAfterCommit(BookingEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void sendBookingReminder(UUID userId, UUID bookingId) {
        notifications.createNotification(userId, "BOOKING_REMINDER", "Booking reminder", "Upcoming booking reminder", "BOOKING", bookingId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingEvent(BookingEvent e) {
        notifications.createNotification(e.teacherId(), e.type(), "Booking update", "Booking status changed", "BOOKING", e.bookingId());
    }
}