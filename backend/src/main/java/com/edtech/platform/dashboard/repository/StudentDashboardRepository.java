package com.edtech.platform.dashboard.repository;

import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StudentDashboardRepository {

    private static final String DASHBOARD_SQL = """
            SELECT
                (SELECT MIN(b.start_time)
                   FROM bookings b
                  WHERE b.student_id = :studentId
                    AND b.status = 'SCHEDULED'
                    AND b.start_time >= CURRENT_TIMESTAMP
                    AND b.is_deleted = false) AS next_booking_start_time,
                COALESCE((SELECT SUM(sp.remaining_sessions)
                            FROM student_packages sp
                           WHERE sp.student_id = :studentId
                             AND sp.status = 'ACTIVE'
                             AND sp.is_deleted = false), 0) AS remaining_sessions,
                (SELECT COUNT(*)
                   FROM assignments a
                  WHERE a.student_id = :studentId
                    AND a.status = 'PUBLISHED'
                    AND a.is_deleted = false
                    AND NOT EXISTS (
                        SELECT 1
                          FROM submissions s
                         WHERE s.assignment_id = a.id
                           AND s.student_id = :studentId
                           AND s.status IN ('SUBMITTED', 'GRADED')
                           AND s.is_deleted = false
                    )) AS todo_assignments,
                (SELECT COUNT(*)
                   FROM notifications n
                  WHERE n.user_id = :studentId
                    AND n.is_read = false) AS unread_notifications,
                (SELECT COUNT(*) FROM trial_requests tr
                  WHERE tr.student_id = :studentId
                    AND tr.status = 'PENDING'
                    AND tr.is_deleted = false)
                + (SELECT COUNT(*) FROM refund_requests rr
                  WHERE rr.student_id = :studentId
                    AND rr.status = 'PENDING'
                    AND rr.is_deleted = false)
                + (SELECT COUNT(*) FROM package_extension_requests er
                  WHERE er.student_id = :studentId
                    AND er.status = 'PENDING'
                    AND er.is_deleted = false) AS pending_requests
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StudentDashboardView getDashboard(UUID studentId) {
        MapSqlParameterSource params = new MapSqlParameterSource("studentId", studentId);
        return jdbcTemplate.queryForObject(DASHBOARD_SQL, params, (rs, rowNum) -> {
            Timestamp nextBooking = rs.getTimestamp("next_booking_start_time");
            return new StudentDashboardView(
                    nextBooking == null ? null : nextBooking.toInstant(),
                    rs.getLong("remaining_sessions"),
                    rs.getLong("todo_assignments"),
                    rs.getLong("unread_notifications"),
                    rs.getLong("pending_requests")
            );
        });
    }
}
