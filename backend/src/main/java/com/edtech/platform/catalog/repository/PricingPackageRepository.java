package com.edtech.platform.catalog.repository;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PricingPackageRepository extends JpaRepository<PricingPackage, UUID> {
    Page<PricingPackage> findByTeacherIdAndStatus(UUID teacherId, PackageStatus status, Pageable pageable);
}
