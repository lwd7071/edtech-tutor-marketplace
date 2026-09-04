package com.edtech.platform.booking.domain;
import com.edtech.platform.common.persistence.BaseEntity;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.Where; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="session_reports", uniqueConstraints=@UniqueConstraint(name="uq_session_reports_booking",columnNames="booking_id")) @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Where(clause="is_deleted=false")
public class SessionReport extends BaseEntity {
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;
    private String recordLink;
    private String content;
    private String feedback;
    @Column(name = "follow_up_note")
    private String followUpNote;
    @Column(name = "teacher_self_rating")
    private Short teacherSelfRating;
    @Column(name = "submitted_at")
    private Instant submittedAt;

    public static SessionReport create(UUID bookingId, String recordLink, String content, String feedback, String followUpNote, Integer rating, Instant submittedAt) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.BOOKING_REPORT_REQUIRED);
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("rating must be 1..5");
        }
        SessionReport r = new SessionReport();
        r.bookingId = bookingId;
        r.recordLink = recordLink;
        r.content = content;
        r.feedback = feedback;
        r.followUpNote = followUpNote;
        r.teacherSelfRating = rating == null ? null : rating.shortValue();
        r.submittedAt = submittedAt;
        return r;
    }
}
