package com.edtech.platform.booking.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="trial_requests") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@SQLDelete(sql="UPDATE trial_requests SET is_deleted=true WHERE id=? AND version=?") @SQLRestriction("is_deleted = false")
public class TrialRequest extends BaseEntity {
 @Column(name="teacher_id",nullable=false) private UUID teacherId;
 @Column(name="student_id",nullable=false) private UUID studentId;
 @Column(name="subject_id",nullable=false) private UUID subjectId;
 @Column(name="preferred_start_time",nullable=false) private Instant preferredStartTime;
 private String note;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private TrialRequestStatus status;
 @Column(name="booking_id") private UUID bookingId;
 @Column(name="rejection_reason") private String rejectionReason;
 @Column(name="responded_at") private Instant respondedAt;
 @Version @Column(nullable = false, columnDefinition = "bigint default 0") private long version;
 public static TrialRequest create(UUID studentId,UUID teacherId,UUID subjectId,Instant preferred,String note){
  if(studentId==null||teacherId==null||subjectId==null||preferred==null) throw new IllegalArgumentException("trial fields required");
  TrialRequest r=new TrialRequest(); r.studentId=studentId;r.teacherId=teacherId;r.subjectId=subjectId;r.preferredStartTime=preferred;r.note=note;r.status=TrialRequestStatus.PENDING; return r;
 }
 public void accept(UUID bookingId, Instant at){ requirePending(); this.bookingId=bookingId;status=TrialRequestStatus.ACCEPTED;respondedAt=at; }
 public void reject(String reason,Instant at){ requirePending(); if(reason==null||reason.isBlank()) throw new BusinessException(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED); rejectionReason=reason;status=TrialRequestStatus.REJECTED;respondedAt=at; }
 private void requirePending(){if(status!=TrialRequestStatus.PENDING) throw new BusinessException(ErrorCode.TRIAL_REQUEST_INVALID_STATE);}
}
