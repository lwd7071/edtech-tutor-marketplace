package com.edtech.platform.booking.dto.response;

import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.domain.SessionReport;

import java.time.Instant;
import java.util.UUID;

public record SessionReportView(
        UUID id,
        UUID bookingId,
        UUID teacherId,
        UUID subjectId,
        Instant startTime,
        Instant endTime,
        DeliveryMode deliveryMode,
        String recordLink,
        String content,
        String feedback,
        String followUpNote,
        Short teacherSelfRating,
        Instant submittedAt
) {
    public static SessionReportView from(SessionReport report, Booking booking) {
        return new SessionReportView(
                report.getId(),
                report.getBookingId(),
                booking != null ? booking.getTeacherId() : null,
                booking != null ? booking.getSubjectId() : null,
                booking != null ? booking.getStartTime() : null,
                booking != null ? booking.getEndTime() : null,
                booking != null ? booking.getDeliveryMode() : null,
                report.getRecordLink(),
                report.getContent(),
                report.getFeedback(),
                report.getFollowUpNote(),
                report.getTeacherSelfRating(),
                report.getSubmittedAt()
        );
    }
}
