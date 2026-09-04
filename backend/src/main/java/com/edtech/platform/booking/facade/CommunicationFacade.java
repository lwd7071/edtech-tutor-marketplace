package com.edtech.platform.booking.facade;

import java.util.UUID;

public interface CommunicationFacade {
    void publishAfterCommit(BookingEvent event);
    void sendBookingReminder(UUID userId, UUID bookingId);
}