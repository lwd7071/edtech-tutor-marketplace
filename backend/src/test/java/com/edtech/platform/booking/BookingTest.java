package com.edtech.platform.booking;

import com.edtech.platform.booking.domain.*;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @Test
    void scheduleOfficial_shouldSucceed_whenValid() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        Booking booking = Booking.scheduleOfficial(
                teacherId, studentId, packageId, subjectId,
                start, end, DeliveryMode.ONLINE, "https://meet.google.com/abc", "Online", false
        );

        assertThat(booking.getTeacherId()).isEqualTo(teacherId);
        assertThat(booking.getStudentId()).isEqualTo(studentId);
        assertThat(booking.getStudentPackageId()).isEqualTo(packageId);
        assertThat(booking.getSubjectId()).isEqualTo(subjectId);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.SCHEDULED);
        assertThat(booking.isTrial()).isFalse();
        assertThat(booking.isDeleted()).isFalse();
    }

    @Test
    void scheduleOfficial_shouldThrow_whenPackageIdIsNull() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        assertThatThrownBy(() -> Booking.scheduleOfficial(
                teacherId, studentId, null, subjectId,
                start, end, DeliveryMode.ONLINE, null, null, false
        )).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.TRIAL_PACKAGE_NOT_ALLOWED));
    }

    @Test
    void scheduleTrial_shouldSucceed_andHaveNullPackageId() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        Booking booking = Booking.scheduleTrial(
                teacherId, studentId, subjectId,
                start, end, DeliveryMode.ONLINE, null, null, false
        );

        assertThat(booking.isTrial()).isTrue();
        assertThat(booking.getStudentPackageId()).isNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.SCHEDULED);
    }

    @Test
    void schedule_shouldThrow_whenInvalidTimeRange() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.minusSeconds(60); // end before start

        assertThatThrownBy(() -> Booking.scheduleOfficial(
                teacherId, studentId, packageId, subjectId,
                start, end, DeliveryMode.ONLINE, null, null, false
        )).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_INVALID_TIME_RANGE));
    }

    @Test
    void complete_shouldTransitionStatus_whenScheduled() {
        Instant start = Instant.now().plusSeconds(100);
        Instant end = start.plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        Instant completedAt = Instant.now();
        booking.complete(completedAt);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(booking.getCompletedAt()).isEqualTo(completedAt);

        // Subsequent complete should throw
        assertThatThrownBy(() -> booking.complete(Instant.now()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_INVALID_STATE));
    }

    @Test
    void cancel_shouldTransitionStatus_andRecordReasonAndInitiator() {
        Instant start = Instant.now().plusSeconds(100);
        Instant end = start.plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        Instant cancelledAt = Instant.now();
        booking.cancel("Teacher emergency", CancelInitiatedBy.TEACHER, cancelledAt);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo("Teacher emergency");
        assertThat(booking.getCancelInitiatedBy()).isEqualTo(CancelInitiatedBy.TEACHER);
        assertThat(booking.getCancelledAt()).isEqualTo(cancelledAt);
        assertThat(booking.isDeleted()).isFalse();

        // Cannot complete cancelled booking
        assertThatThrownBy(() -> booking.complete(Instant.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_shouldThrow_whenReasonIsBlank() {
        Instant start = Instant.now().plusSeconds(100);
        Instant end = start.plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        assertThatThrownBy(() -> booking.cancel("   ", CancelInitiatedBy.TEACHER, Instant.now()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED));
    }

    @Test
    void expire_shouldTransitionStatus_andSetSystemInitiator() {
        Instant start = Instant.now().plusSeconds(100);
        Instant end = start.plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        Instant expiredAt = Instant.now();
        booking.expire(expiredAt);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(booking.getCancelInitiatedBy()).isEqualTo(CancelInitiatedBy.SYSTEM);
        assertThat(booking.getExpiredAt()).isEqualTo(expiredAt);
    }

    @Test
    void markSettlementProcessed_shouldSetFlag_andThrowOnDuplicate() {
        Instant start = Instant.now().plusSeconds(100);
        Instant end = start.plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        assertThat(booking.isSettlementProcessed()).isFalse();
        booking.markSettlementProcessed();
        assertThat(booking.isSettlementProcessed()).isTrue();

        assertThatThrownBy(booking::markSettlementProcessed)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_SETTLEMENT_ALREADY_PROCESSED));
    }
}