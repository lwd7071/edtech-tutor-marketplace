package com.edtech.platform.booking.repository;

import com.edtech.platform.booking.domain.BookingRequest;
import com.edtech.platform.booking.domain.BookingRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, UUID> {
    boolean existsByStudentPackageIdAndStatus(UUID studentPackageId, BookingRequestStatus status);
    Page<BookingRequest> findByStudentId(UUID studentId, Pageable pageable);
    Page<BookingRequest> findByStudentIdAndStatus(UUID studentId, BookingRequestStatus status, Pageable pageable);
    Page<BookingRequest> findByTeacherId(UUID teacherId, Pageable pageable);
    Page<BookingRequest> findByTeacherIdAndStatus(UUID teacherId, BookingRequestStatus status, Pageable pageable);
    Optional<BookingRequest> findFirstByStudentPackageIdAndStudentIdAndStatusOrderByCreatedAtDesc(
            UUID studentPackageId, UUID studentId, BookingRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from BookingRequest request where request.id = :id")
    Optional<BookingRequest> findByIdForUpdate(UUID id);
}
