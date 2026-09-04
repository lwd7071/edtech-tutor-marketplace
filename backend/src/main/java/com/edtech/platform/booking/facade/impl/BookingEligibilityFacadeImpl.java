package com.edtech.platform.booking.facade.impl;

import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.booking.facade.dto.BookingStatsSnapshot;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.TrialRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingEligibilityFacadeImpl implements BookingEligibilityFacade {

    private final BookingRepository bookingRepository;
    private final TrialRequestRepository trialRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> getTeacherIdForReviewableBooking(UUID studentId, UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && studentId.equals(b.getStudentId()))
                .map(Booking::getTeacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatsSnapshot getTeacherBookingStats(UUID teacherId) {
        int completedSessions = bookingRepository.countCompletedSessionsByTeacherId(teacherId);
        int totalSessions = bookingRepository.countTotalSessionsByTeacherId(teacherId);
        int trialSessions = bookingRepository.countTrialCompletedSessionsByTeacherId(teacherId);

        return new BookingStatsSnapshot(completedSessions, totalSessions, trialSessions);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidBookingOrTrial(UUID teacherId, UUID studentId) {
        boolean hasBooking = bookingRepository.existsByTeacherIdAndStudentIdAndStatusIn(
                teacherId,
                studentId,
                List.of(BookingStatus.SCHEDULED, BookingStatus.COMPLETED)
        );
        if (hasBooking) {
            return true;
        }
        return trialRequestRepository.existsByTeacherIdAndStudentIdAndStatus(
                teacherId,
                studentId,
                TrialRequestStatus.ACCEPTED
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasScheduledBookingForPackage(UUID studentPackageId) {
        return bookingRepository.existsByStudentPackageIdAndStatusAndDeletedFalse(
                studentPackageId,
                BookingStatus.SCHEDULED
        );
    }
}
