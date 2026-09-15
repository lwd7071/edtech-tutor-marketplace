package com.edtech.platform.booking.repository;
import com.edtech.platform.booking.domain.BookingSettlement;
import com.edtech.platform.booking.domain.SettlementStatus;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface BookingSettlementRepository extends JpaRepository<BookingSettlement, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from BookingSettlement s where s.id = :id")
    Optional<BookingSettlement> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<BookingSettlement> findByBookingId(UUID bookingId);
    @Query("select s from BookingSettlement s where s.bookingId = :bookingId") Optional<BookingSettlement> findByBookingIdForRead(@org.springframework.data.repository.query.Param("bookingId") UUID bookingId);
    @Query("select s from BookingSettlement s where s.status = com.edtech.platform.booking.domain.SettlementStatus.AWAITING_CONFIRMATION and s.initialDeadline <= :at") List<BookingSettlement> findInitialExpired(java.time.Instant at, org.springframework.data.domain.Pageable pageable);
    @Query("select s from BookingSettlement s where s.status = com.edtech.platform.booking.domain.SettlementStatus.REOPENED and s.reopenDeadline <= :at") List<BookingSettlement> findReopenedExpired(java.time.Instant at, org.springframework.data.domain.Pageable pageable);
    @Query("select s from BookingSettlement s where (:status is null or s.status = :status)")
    org.springframework.data.domain.Page<BookingSettlement> findByStatus(@org.springframework.data.repository.query.Param("status") SettlementStatus status, org.springframework.data.domain.Pageable pageable);
}
