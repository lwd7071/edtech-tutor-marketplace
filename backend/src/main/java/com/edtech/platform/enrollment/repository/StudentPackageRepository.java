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
    @Query("select p.studentId, max(p.startsAt) from StudentPackage p where p.teacherId=:teacherId and p.deleted=false group by p.studentId")
    java.util.List<Object[]> latestPurchaseTimes(UUID teacherId);
    Page<StudentPackage> findByTeacherId(UUID teacherId, Pageable pageable);
    Page<StudentPackage> findByTeacherIdAndStudentId(UUID teacherId, UUID studentId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StudentPackage p where p.id = :id")
    Optional<StudentPackage> findByIdForUpdate(UUID id);
    Optional<StudentPackage> findByInvoiceId(UUID invoiceId);
    Page<StudentPackage> findByStudentIdAndStatus(UUID studentId, StudentPackageStatus status, Pageable pageable);
    Optional<StudentPackage> findByIdAndStudentId(UUID id, UUID studentId);
    Page<StudentPackage> findByStudentId(UUID studentId, Pageable pageable);
    @Query("select p from StudentPackage p where p.status = com.edtech.platform.enrollment.domain.StudentPackageStatus.ACTIVE and p.expiresAt <= :now")
    Page<StudentPackage> findExpired(java.time.Instant now, Pageable pageable);

    boolean existsByTeacherIdAndStudentIdAndStatusInAndDeletedFalse(UUID teacherId, UUID studentId, java.util.Collection<StudentPackageStatus> statuses);

    boolean existsByPricingPackageIdAndDeletedFalse(UUID pricingPackageId);
}
