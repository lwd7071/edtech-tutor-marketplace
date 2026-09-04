package com.edtech.platform.finance.repository;

import com.edtech.platform.finance.domain.PayoutRequest;
import com.edtech.platform.finance.domain.PayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    Page<PayoutRequest> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

    Page<PayoutRequest> findByTeacherIdAndStatusOrderByCreatedAtDesc(UUID teacherId, PayoutStatus status, Pageable pageable);

    Page<PayoutRequest> findByStatusOrderByCreatedAtDesc(PayoutStatus status, Pageable pageable);

    Page<PayoutRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByTeacherIdAndStatusIn(UUID teacherId, Collection<PayoutStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayoutRequest p WHERE p.id = :id")
    Optional<PayoutRequest> findByIdForUpdate(@Param("id") UUID id);
}