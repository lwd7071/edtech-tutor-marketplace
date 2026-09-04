package com.edtech.platform.booking;

import com.edtech.platform.booking.domain.SessionReport;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionReportTest {

    @Test
    void create_shouldSucceed_whenValid() {
        UUID bookingId = UUID.randomUUID();
        Instant now = Instant.now();
        SessionReport report = SessionReport.create(
                bookingId, "https://rec.example.com/123", "Covered Chapter 1", "Good progress", "Practice homework", 5, now
        );

        assertThat(report.getBookingId()).isEqualTo(bookingId);
        assertThat(report.getContent()).isEqualTo("Covered Chapter 1");
        assertThat(report.getTeacherSelfRating()).isEqualTo((short) 5);
        assertThat(report.getSubmittedAt()).isEqualTo(now);
    }

    @Test
    void create_shouldThrow_whenContentIsBlank() {
        UUID bookingId = UUID.randomUUID();
        assertThatThrownBy(() -> SessionReport.create(bookingId, null, "  ", null, null, 4, Instant.now()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_REPORT_REQUIRED));
    }

    @Test
    void create_shouldThrow_whenRatingOutOfRange() {
        UUID bookingId = UUID.randomUUID();
        assertThatThrownBy(() -> SessionReport.create(bookingId, null, "Content", null, null, 6, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SessionReport.create(bookingId, null, "Content", null, null, 0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}