package com.edtech.platform.booking.facade;

import com.edtech.platform.booking.facade.dto.BookingPackageSnapshot;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface EnrollmentBookingFacade {
    BookingPackageSnapshot inspect(UUID packageId, UUID studentId);
    void reserveSession(UUID packageId);
    void completeReservedSession(UUID packageId);
    void releaseReservedSession(UUID packageId);
    void lockExpiredPackages(Instant cutoff, Pageable pageable);
}