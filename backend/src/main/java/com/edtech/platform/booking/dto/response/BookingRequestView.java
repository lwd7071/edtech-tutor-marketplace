package com.edtech.platform.booking.dto.response;

import com.edtech.platform.booking.domain.BookingRequest;
import com.edtech.platform.booking.domain.BookingRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingRequestView(
        UUID id,
        UUID studentPackageId,
        UUID studentId,
        UUID teacherId,
        UUID subjectId,
        Instant preferredStartTime,
        String studentNote,
        BookingRequestStatus status,
        String decisionReason,
        UUID acceptedBookingId,
        Instant createdAt,
        long version
) {
    public static BookingRequestView from(BookingRequest request) {
        return new BookingRequestView(request.getId(), request.getStudentPackageId(), request.getStudentId(),
                request.getTeacherId(), request.getSubjectId(), request.getPreferredStartTime(),
                request.getStudentNote(), request.getStatus(), request.getDecisionReason(),
                request.getAcceptedBookingId(), request.getCreatedAt(), request.getVersion());
    }
}
