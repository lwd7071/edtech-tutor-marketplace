package com.edtech.platform.booking.dto.response;

import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.SettlementStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingSettlementAdminView(
        UUID bookingId,
        UUID studentId,
        String studentName,
        UUID teacherId,
        String teacherName,
        BookingStatus bookingStatus,
        Instant startTime,
        Instant endTime,
        SettlementStatus status,
        Instant teacherConfirmedAt,
        Instant studentConfirmedAt,
        Instant confirmationDeadline,
        Instant reopenDeadline,
        Long netAmountVnd,
        String disputeReason,
        Instant disputedAt,
        long version
) {}
