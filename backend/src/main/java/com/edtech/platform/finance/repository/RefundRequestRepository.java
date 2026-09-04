package com.edtech.platform.finance.repository;

import com.edtech.platform.finance.domain.RefundRequest;
import com.edtech.platform.finance.domain.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    Page<RefundRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    Page<RefundRequest> findByStatusOrderByCreatedAtDesc(RefundStatus status, Pageable pageable);

    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByStudentPackageIdAndStatus(UUID studentPackageId, RefundStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundRequest r WHERE r.id = :id")
    Optional<RefundRequest> findByIdForUpdate(@Param("id") UUID id);
}