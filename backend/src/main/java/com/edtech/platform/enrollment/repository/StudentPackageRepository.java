package com.edtech.platform.enrollment.repository;

import com.edtech.platform.enrollment.domain.StudentPackage;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface StudentPackageRepository extends JpaRepository<StudentPackage, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StudentPackage p where p.id = :id")
    Optional<StudentPackage> findByIdForUpdate(UUID id);
    Optional<StudentPackage> findByInvoiceId(UUID invoiceId);
    Page<StudentPackage> findByStudentIdAndStatus(UUID studentId, StudentPackageStatus status, Pageable pageable);
    Optional<StudentPackage> findByIdAndStudentId(UUID id, UUID studentId);
}
