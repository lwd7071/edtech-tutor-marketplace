package com.edtech.platform.booking.facade.impl;

import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.booking.facade.dto.BookingStatsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingEligibilityFacadeImpl implements BookingEligibilityFacade {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<UUID> getTeacherIdForReviewableBooking(UUID studentId, UUID bookingId) {
        String sql = "SELECT status, student_id, teacher_id FROM bookings WHERE id = ? AND is_deleted = false";
        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(sql, bookingId);
        
        if (bookings.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> booking = bookings.get(0);
        String status = (String) booking.get("status");
        UUID bookingStudentId = (UUID) booking.get("student_id");
        UUID bookingTeacherId = (UUID) booking.get("teacher_id");
        
        if ("COMPLETED".equals(status) && studentId.equals(bookingStudentId)) {
            return Optional.of(bookingTeacherId);
        }
        return Optional.empty();
    }

    @Override
    public BookingStatsSnapshot getTeacherBookingStats(UUID teacherId) {
        Map<String, Object> bookingStats = jdbcTemplate.queryForMap(
                "SELECT " +
                "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_sessions, " +
                "COUNT(CASE WHEN status IN ('COMPLETED', 'CANCELLED', 'SCHEDULED') THEN 1 END) as total_sessions, " +
                "COUNT(CASE WHEN is_trial = true AND status = 'COMPLETED' THEN 1 END) as trial_sessions " +
                "FROM bookings WHERE teacher_id = ? AND is_deleted = false", teacherId);
        
        int completedSessions = ((Number) bookingStats.get("completed_sessions")).intValue();
        int totalSessions = ((Number) bookingStats.get("total_sessions")).intValue();
        int trialSessions = ((Number) bookingStats.get("trial_sessions")).intValue();
        
        return new BookingStatsSnapshot(completedSessions, totalSessions, trialSessions);
    }
}
