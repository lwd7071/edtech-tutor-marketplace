package com.edtech.platform.booking.facade;

import com.edtech.platform.booking.facade.dto.BookingStatsSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface BookingEligibilityFacade {
    Optional<UUID> getTeacherIdForReviewableBooking(UUID studentId, UUID bookingId);
    BookingStatsSnapshot getTeacherBookingStats(UUID teacherId);
}
