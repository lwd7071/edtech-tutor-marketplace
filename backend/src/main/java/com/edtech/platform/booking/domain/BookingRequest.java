package com.edtech.platform.booking.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "booking_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE booking_requests SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class BookingRequest extends BaseEntity {
    @Column(name = "student_package_id", nullable = false) private UUID studentPackageId;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "teacher_id", nullable = false) private UUID teacherId;
    @Column(name = "subject_id", nullable = false) private UUID subjectId;
    @Column(name = "preferred_start_time", nullable = false) private Instant preferredStartTime;
    @Column(name = "student_note", length = 500) private String studentNote;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingRequestStatus status;
    @Column(name = "decision_reason", columnDefinition = "text") private String decisionReason;
    @Column(name = "accepted_booking_id", unique = true) private UUID acceptedBookingId;
    @Version @Column(nullable = false) private long version;

    public static BookingRequest create(UUID packageId, UUID studentId, UUID teacherId, UUID subjectId,
                                        Instant preferredStartTime, String studentNote) {
        BookingRequest request = new BookingRequest();
        request.studentPackageId = Objects.requireNonNull(packageId);
        request.studentId = Objects.requireNonNull(studentId);
        request.teacherId = Objects.requireNonNull(teacherId);
        request.subjectId = Objects.requireNonNull(subjectId);
        request.preferredStartTime = Objects.requireNonNull(preferredStartTime);
        request.studentNote = studentNote;
        request.status = BookingRequestStatus.PENDING;
        return request;
    }

    public void cancel() {
        requirePending();
        status = BookingRequestStatus.CANCELLED;
    }

    public void accept(UUID bookingId) {
        requirePending();
        acceptedBookingId = Objects.requireNonNull(bookingId);
        status = BookingRequestStatus.ACCEPTED;
    }

    public void reject(String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_REASON_REQUIRED);
        }
        decisionReason = reason.trim();
        status = BookingRequestStatus.REJECTED;
    }

    private void requirePending() {
        if (status != BookingRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_INVALID_STATE);
        }
    }
}
