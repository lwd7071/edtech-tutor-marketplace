package com.edtech.platform.booking.facade.impl;

import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.facade.dto.BookingStatsSnapshot;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.TrialRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEligibilityFacadeImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TrialRequestRepository trialRequestRepository;

    @InjectMocks
    private BookingEligibilityFacadeImpl bookingEligibilityFacade;

    private UUID teacherId;
    private UUID studentId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    void getTeacherIdForReviewableBooking_shouldReturnTeacherId_whenCompletedAndStudentMatches() {
        Booking booking = Booking.scheduleOfficial(
                teacherId, studentId, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600),
                DeliveryMode.ONLINE, "link", null, false
        );
        booking.complete(Instant.now().minusSeconds(3600));

        when(bookingRepository.findById(eq(bookingId))).thenReturn(Optional.of(booking));

        Optional<UUID> result = bookingEligibilityFacade.getTeacherIdForReviewableBooking(studentId, bookingId);

        assertThat(result).contains(teacherId);
    }

    @Test
    void getTeacherIdForReviewableBooking_shouldReturnEmpty_whenNotCompleted() {
        Booking booking = Booking.scheduleOfficial(
                teacherId, studentId, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200),
                DeliveryMode.ONLINE, "link", null, false
        );

        when(bookingRepository.findById(eq(bookingId))).thenReturn(Optional.of(booking));

        Optional<UUID> result = bookingEligibilityFacade.getTeacherIdForReviewableBooking(studentId, bookingId);

        assertThat(result).isEmpty();
    }

    @Test
    void getTeacherIdForReviewableBooking_shouldReturnEmpty_whenStudentMismatch() {
        Booking booking = Booking.scheduleOfficial(
                teacherId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600),
                DeliveryMode.ONLINE, "link", null, false
        );
        booking.complete(Instant.now().minusSeconds(3600));

        when(bookingRepository.findById(eq(bookingId))).thenReturn(Optional.of(booking));

        Optional<UUID> result = bookingEligibilityFacade.getTeacherIdForReviewableBooking(studentId, bookingId);

        assertThat(result).isEmpty();
    }

    @Test
    void getTeacherBookingStats_shouldAggregateStatsCorrectly() {
        when(bookingRepository.countCompletedSessionsByTeacherId(teacherId)).thenReturn(10);
        when(bookingRepository.countTotalSessionsByTeacherId(teacherId)).thenReturn(12);
        when(bookingRepository.countTrialCompletedSessionsByTeacherId(teacherId)).thenReturn(3);

        BookingStatsSnapshot stats = bookingEligibilityFacade.getTeacherBookingStats(teacherId);

        assertThat(stats.completedSessions()).isEqualTo(10);
        assertThat(stats.totalSessions()).isEqualTo(12);
        assertThat(stats.trialSessions()).isEqualTo(3);
    }

    @Test
    void hasValidBookingOrTrial_shouldReturnTrue_whenHasScheduledOrCompletedBooking() {
        when(bookingRepository.existsByTeacherIdAndStudentIdAndStatusIn(
                eq(teacherId), eq(studentId), eq(List.of(BookingStatus.SCHEDULED, BookingStatus.COMPLETED))))
                .thenReturn(true);

        boolean result = bookingEligibilityFacade.hasValidBookingOrTrial(teacherId, studentId);

        assertThat(result).isTrue();
    }

    @Test
    void hasValidBookingOrTrial_shouldFallbackToTrialRequest_whenNoActiveBooking() {
        when(bookingRepository.existsByTeacherIdAndStudentIdAndStatusIn(
                eq(teacherId), eq(studentId), eq(List.of(BookingStatus.SCHEDULED, BookingStatus.COMPLETED))))
                .thenReturn(false);
        when(trialRequestRepository.existsByTeacherIdAndStudentIdAndStatus(
                eq(teacherId), eq(studentId), eq(TrialRequestStatus.ACCEPTED)))
                .thenReturn(true);

        boolean result = bookingEligibilityFacade.hasValidBookingOrTrial(teacherId, studentId);

        assertThat(result).isTrue();
    }

    @Test
    void hasValidBookingOrTrial_shouldReturnFalse_whenNeitherExists() {
        when(bookingRepository.existsByTeacherIdAndStudentIdAndStatusIn(
                eq(teacherId), eq(studentId), eq(List.of(BookingStatus.SCHEDULED, BookingStatus.COMPLETED))))
                .thenReturn(false);
        when(trialRequestRepository.existsByTeacherIdAndStudentIdAndStatus(
                eq(teacherId), eq(studentId), eq(TrialRequestStatus.ACCEPTED)))
                .thenReturn(false);

        boolean result = bookingEligibilityFacade.hasValidBookingOrTrial(teacherId, studentId);

        assertThat(result).isFalse();
    }
}