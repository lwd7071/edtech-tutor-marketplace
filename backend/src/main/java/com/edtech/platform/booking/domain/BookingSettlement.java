package com.edtech.platform.booking.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="booking_settlements", uniqueConstraints=@UniqueConstraint(name="uq_booking_settlements_booking", columnNames="booking_id"))
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class BookingSettlement extends BaseEntity {
    @Column(name="booking_id", nullable=false, unique=true) private UUID bookingId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private SettlementStatus status;
    @Column(name="teacher_confirmed_at") private Instant teacherConfirmedAt;
    @Column(name="student_confirmed_at") private Instant studentConfirmedAt;
    @Column(name="initial_deadline", nullable=false) private Instant initialDeadline;
    @Column(name="reopen_deadline") private Instant reopenDeadline;
    @Column(name="net_amount_vnd") private Long netAmountVnd;
    @Column(name="dispute_reason") private String disputeReason;
    @Column(name="disputed_at") private Instant disputedAt;
    @Column(name="reopened_at") private Instant reopenedAt;
    @Version @Column(nullable=false) private long version;

    public static BookingSettlement awaiting(UUID bookingId, Instant deadline, Long amount) {
        BookingSettlement s = new BookingSettlement(); s.bookingId=bookingId; s.status=SettlementStatus.AWAITING_CONFIRMATION;
        s.initialDeadline=deadline; s.netAmountVnd=amount; return s;
    }
    public void confirmTeacher(Instant now) { requireConfirmationWindow(now); if (teacherConfirmedAt != null) throw new BusinessException(ErrorCode.BOOKING_CONFIRMATION_ALREADY_EXISTS); teacherConfirmedAt=now; }
    public void confirmStudent(Instant now) { requireConfirmationWindow(now); if (studentConfirmedAt != null) throw new BusinessException(ErrorCode.BOOKING_CONFIRMATION_ALREADY_EXISTS); studentConfirmedAt=now; }
    public void requireConfirmationWindow(Instant now) {
        Instant deadline = status == SettlementStatus.REOPENED ? reopenDeadline : initialDeadline;
        if (status != SettlementStatus.REOPENED && status != SettlementStatus.AWAITING_CONFIRMATION)
            throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE);
        if (deadline == null || !now.isBefore(deadline))
            throw new BusinessException(ErrorCode.BOOKING_CONFIRMATION_EXPIRED);
    }
    public boolean bothConfirmed() { return teacherConfirmedAt != null && studentConfirmedAt != null; }
    public void hold() { if (status != SettlementStatus.AWAITING_CONFIRMATION) throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE); status=SettlementStatus.HELD; }
    public void dispute(String reason, Instant now) { if(status != SettlementStatus.HELD) throw new BusinessException(ErrorCode.BOOKING_DISPUTE_INVALID_STATE); if(reason==null||reason.isBlank()) throw new BusinessException(ErrorCode.BOOKING_DISPUTE_REASON_REQUIRED); disputeReason=reason; disputedAt=now; status=SettlementStatus.DISPUTE_PENDING; }
    public void reopen(Instant now) { if(status != SettlementStatus.DISPUTE_PENDING || reopenedAt != null) throw new BusinessException(ErrorCode.BOOKING_REOPEN_INVALID_STATE); reopenedAt=now; reopenDeadline=now.plusSeconds(86400); status=SettlementStatus.REOPENED; }
    public void release() { if(status != SettlementStatus.AWAITING_CONFIRMATION && status != SettlementStatus.REOPENED) throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE); status=SettlementStatus.RELEASED; }
    public void adminDecision(boolean retain) {
        if (status == SettlementStatus.DISPUTE_PENDING && retain) { status = SettlementStatus.RETAINED; return; }
        if(status != SettlementStatus.AWAITING_ADMIN_DECISION) throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE);
        status=retain?SettlementStatus.RETAINED:SettlementStatus.RELEASED;
    }
    public void awaitingAdminDecision() { if(status != SettlementStatus.REOPENED) throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE); status=SettlementStatus.AWAITING_ADMIN_DECISION; }
    public void allocateAmount(long amount) { if (amount < 0) throw new IllegalArgumentException("amount must be non-negative"); if (netAmountVnd != null && netAmountVnd != amount) throw new IllegalStateException("settlement amount already allocated"); if (netAmountVnd == null) netAmountVnd = amount; }
}
