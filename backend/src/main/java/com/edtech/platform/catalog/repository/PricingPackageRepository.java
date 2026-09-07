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
    Page<PricingPackage> findByTeacherId(UUID teacherId, Pageable pageable);
    Page<PricingPackage> findByTeacherIdAndStatus(UUID teacherId, PackageStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.teacherId FROM PricingPackage p WHERE p.status = 'ACTIVE' AND p.deleted = false AND (:minPrice IS NULL OR p.priceVnd >= :minPrice) AND (:maxPrice IS NULL OR p.priceVnd <= :maxPrice)")
    java.util.Set<UUID> searchTeacherIdsByPrice(@org.springframework.data.repository.query.Param("minPrice") Long minPrice, @org.springframework.data.repository.query.Param("maxPrice") Long maxPrice);
}
