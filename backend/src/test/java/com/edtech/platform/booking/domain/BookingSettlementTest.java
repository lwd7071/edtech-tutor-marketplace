package com.edtech.platform.booking.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingSettlementTest {
    private final Instant deadline = Instant.parse("2026-09-14T12:00:00Z");

    @Test
    void twoConfirmationsReleaseOnlyWhenBothExist() {
        BookingSettlement s = BookingSettlement.awaiting(UUID.randomUUID(), deadline, null);
        s.confirmStudent(deadline.minusSeconds(10));
        assertThat(s.bothConfirmed()).isFalse();
        s.confirmTeacher(deadline.minusSeconds(1));
        assertThat(s.bothConfirmed()).isTrue();
        s.allocateAmount(0);
        s.release();
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.RELEASED);
        assertThat(s.getNetAmountVnd()).isZero();
    }

    @Test
    void exactDeadlineAndHeldStateRejectConfirmation() {
        BookingSettlement s = BookingSettlement.awaiting(UUID.randomUUID(), deadline, null);
        assertThatThrownBy(() -> s.confirmStudent(deadline)).isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.BOOKING_CONFIRMATION_EXPIRED));
        s.hold();
        assertThatThrownBy(() -> s.confirmTeacher(deadline.plusSeconds(1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void disputeReopenAndSecondDeadlineOnlyAllowMissingSideOnce() {
        BookingSettlement s = BookingSettlement.awaiting(UUID.randomUUID(), deadline, null);
        s.confirmTeacher(deadline.minusSeconds(1));
        s.allocateAmount(100);
        s.hold();
        s.dispute("Thiếu xác nhận của học viên", deadline.plusSeconds(1));
        s.reopen(deadline.plusSeconds(2));
        assertThatThrownBy(() -> s.confirmTeacher(deadline.plusSeconds(3)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.BOOKING_CONFIRMATION_ALREADY_EXISTS));
        assertThatThrownBy(() -> s.confirmStudent(s.getReopenDeadline()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.BOOKING_CONFIRMATION_EXPIRED));
        s.awaitingAdminDecision();
        s.adminDecision(false);
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.RELEASED);
    }

    @Test
    void adminCanRejectDisputeButCannotReopenAfterRetain() {
        BookingSettlement s = BookingSettlement.awaiting(UUID.randomUUID(), deadline, null);
        s.allocateAmount(100);
        s.hold();
        s.dispute("Bằng chứng không đủ", deadline.plusSeconds(1));
        s.adminDecision(true);
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.RETAINED);
        assertThatThrownBy(() -> s.reopen(deadline.plusSeconds(2)))
                .isInstanceOf(BusinessException.class);
    }
}
