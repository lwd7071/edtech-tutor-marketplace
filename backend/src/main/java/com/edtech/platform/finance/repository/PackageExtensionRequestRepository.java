package com.edtech.platform.finance.repository;

import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.domain.PackageExtensionRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PackageExtensionRequestRepository extends JpaRepository<PackageExtensionRequest, UUID> {

    Page<PackageExtensionRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    Page<PackageExtensionRequest> findByStatusOrderByCreatedAtDesc(ExtensionStatus status, Pageable pageable);

    Page<PackageExtensionRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByStudentPackageIdAndStatus(UUID studentPackageId, ExtensionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM PackageExtensionRequest e WHERE e.id = :id")
    Optional<PackageExtensionRequest> findByIdForUpdate(@Param("id") UUID id);
}