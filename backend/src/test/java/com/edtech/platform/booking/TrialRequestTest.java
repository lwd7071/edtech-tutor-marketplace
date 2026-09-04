package com.edtech.platform.booking;

import com.edtech.platform.booking.domain.TrialRequest;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrialRequestTest {

    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @Test
    void create_shouldInitializeInPendingStatus() {
        Instant preferred = Instant.now().plusSeconds(7200);
        TrialRequest req = TrialRequest.create(studentId, teacherId, subjectId, preferred, "Please teach me Math");

        assertThat(req.getStudentId()).isEqualTo(studentId);
        assertThat(req.getTeacherId()).isEqualTo(teacherId);
        assertThat(req.getSubjectId()).isEqualTo(subjectId);
        assertThat(req.getStatus()).isEqualTo(TrialRequestStatus.PENDING);
        assertThat(req.getNote()).isEqualTo("Please teach me Math");
    }

    @Test
    void accept_shouldTransitionToAccepted_andSetBookingId() {
        TrialRequest req = TrialRequest.create(studentId, teacherId, subjectId, Instant.now().plusSeconds(7200), "Note");
        UUID bookingId = UUID.randomUUID();
        Instant now = Instant.now();

        req.accept(bookingId, now);

        assertThat(req.getStatus()).isEqualTo(TrialRequestStatus.ACCEPTED);
        assertThat(req.getBookingId()).isEqualTo(bookingId);
        assertThat(req.getRespondedAt()).isEqualTo(now);

        // Cannot reject or accept again once accepted
        assertThatThrownBy(() -> req.reject("Reason", Instant.now()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.TRIAL_REQUEST_INVALID_STATE));
    }

    @Test
    void reject_shouldTransitionToRejected_andRequireReason() {
        TrialRequest req = TrialRequest.create(studentId, teacherId, subjectId, Instant.now().plusSeconds(7200), "Note");
        Instant now = Instant.now();

        req.reject("Teacher busy at that time", now);

        assertThat(req.getStatus()).isEqualTo(TrialRequestStatus.REJECTED);
        assertThat(req.getRejectionReason()).isEqualTo("Teacher busy at that time");
        assertThat(req.getRespondedAt()).isEqualTo(now);

        // Blank reason should throw
        TrialRequest req2 = TrialRequest.create(studentId, teacherId, subjectId, Instant.now().plusSeconds(7200), "Note");
        assertThatThrownBy(() -> req2.reject("   ", Instant.now()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED));
    }
}