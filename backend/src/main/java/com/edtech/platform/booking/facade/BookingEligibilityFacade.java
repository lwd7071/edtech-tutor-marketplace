package com.edtech.platform.booking.facade;

import com.edtech.platform.booking.facade.dto.BookingStatsSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface BookingEligibilityFacade {
    java.util.Map<UUID, java.time.Instant> firstCompletedTrials(UUID teacherId);
    Optional<UUID> getTeacherIdForReviewableBooking(UUID studentId, UUID bookingId);
    BookingStatsSnapshot getTeacherBookingStats(UUID teacherId);
    boolean hasValidBookingOrTrial(UUID teacherId, UUID studentId);
    boolean hasScheduledBookingForPackage(UUID studentPackageId);
}
