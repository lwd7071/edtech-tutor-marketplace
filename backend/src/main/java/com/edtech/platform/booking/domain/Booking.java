package com.edtech.platform.booking.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity @Table(name="bookings") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@SQLDelete(sql="UPDATE bookings SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class Booking extends BaseEntity {
 @Column(name="teacher_id",nullable=false) private UUID teacherId;
 @Column(name="student_id",nullable=false) private UUID studentId;
 @Column(name="student_package_id") private UUID studentPackageId;
 @Column(name="subject_id",nullable=false) private UUID subjectId;
 @Column(name="start_time",nullable=false) private Instant startTime;
 @Column(name="end_time",nullable=false) private Instant endTime;
 @Enumerated(EnumType.STRING) @Column(name="delivery_mode",nullable=false) private DeliveryMode deliveryMode;
 @Column(name="meeting_link") private String meetingLink;
 @Column(name="location_address") private String locationAddress;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private BookingStatus status;
 @Column(name="is_trial",nullable=false) private boolean trial;
 @Column(name="outside_availability_warning",nullable=false) private boolean outsideAvailabilityWarning;
 @Column(name="cancel_reason") private String cancelReason;
 @Enumerated(EnumType.STRING) @Column(name="cancel_initiated_by") private CancelInitiatedBy cancelInitiatedBy;
 @Column(name="completed_at") private Instant completedAt;
 @Column(name="cancelled_at") private Instant cancelledAt;
 @Column(name="expired_at") private Instant expiredAt;
 @Column(name="settlement_processed",nullable=false) private boolean settlementProcessed;
 @Version @Column(nullable=false) private long version;

 public static Booking scheduleOfficial(UUID teacherId, UUID studentId, UUID packageId, UUID subjectId, Instant start, Instant end, DeliveryMode mode, String meetingLink, String location, boolean warning) {
  if (packageId == null) throw invalid(ErrorCode.TRIAL_PACKAGE_NOT_ALLOWED);
  return create(teacherId,studentId,packageId,subjectId,start,end,mode,meetingLink,location,false,warning);
 }
 public static Booking scheduleTrial(UUID teacherId, UUID studentId, UUID subjectId, Instant start, Instant end, DeliveryMode mode, String meetingLink, String location, boolean warning) {
  return create(teacherId,studentId,null,subjectId,start,end,mode,meetingLink,location,true,warning);
 }
 private static Booking create(UUID teacherId, UUID studentId, UUID packageId, UUID subjectId, Instant start, Instant end, DeliveryMode mode, String link, String location, boolean trial, boolean warning) {
  Objects.requireNonNull(teacherId); Objects.requireNonNull(studentId); Objects.requireNonNull(subjectId); Objects.requireNonNull(start); Objects.requireNonNull(end); Objects.requireNonNull(mode);
  if (!start.isBefore(end)) throw invalid(ErrorCode.BOOKING_INVALID_TIME_RANGE);
  Booking b=new Booking(); b.teacherId=teacherId; b.studentId=studentId; b.studentPackageId=packageId; b.subjectId=subjectId; b.startTime=start; b.endTime=end; b.deliveryMode=mode; b.meetingLink=link; b.locationAddress=location; b.trial=trial; b.outsideAvailabilityWarning=warning; b.status=BookingStatus.SCHEDULED; return b;
 }
 public void complete(Instant at) { require(BookingStatus.SCHEDULED); completedAt=Objects.requireNonNull(at); status=BookingStatus.COMPLETED; }
 public void cancel(String reason, CancelInitiatedBy by, Instant at) { require(BookingStatus.SCHEDULED); if(reason==null||reason.isBlank()) throw invalid(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED); cancelReason=reason; cancelInitiatedBy=Objects.requireNonNull(by); cancelledAt=Objects.requireNonNull(at); status=BookingStatus.CANCELLED; }
 public void expire(Instant at) { require(BookingStatus.SCHEDULED); expiredAt=Objects.requireNonNull(at); cancelInitiatedBy=CancelInitiatedBy.SYSTEM; status=BookingStatus.EXPIRED; }
 public void markSettlementProcessed() { if (settlementProcessed) throw invalid(ErrorCode.BOOKING_SETTLEMENT_ALREADY_PROCESSED); settlementProcessed=true; }
 private void require(BookingStatus expected){if(status!=expected) throw invalid(ErrorCode.BOOKING_INVALID_STATE);}
 private static BusinessException invalid(ErrorCode code){return new BusinessException(code);}
}
