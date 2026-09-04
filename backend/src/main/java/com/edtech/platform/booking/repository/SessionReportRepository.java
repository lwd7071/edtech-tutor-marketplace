package com.edtech.platform.booking.repository;

import com.edtech.platform.booking.domain.SessionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionReportRepository extends JpaRepository<SessionReport, UUID> {
    boolean existsByBookingId(UUID bookingId);

    Optional<SessionReport> findByBookingId(UUID bookingId);

    @Query(value = "SELECT r FROM SessionReport r, Booking b WHERE r.bookingId = b.id AND b.studentId = :studentId",
           countQuery = "SELECT count(r) FROM SessionReport r, Booking b WHERE r.bookingId = b.id AND b.studentId = :studentId")
    Page<SessionReport> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);
}
