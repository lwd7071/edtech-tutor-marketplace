package com.edtech.platform.catalog.facade.impl;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.catalog.facade.PricingPackageFacade;
import com.edtech.platform.catalog.facade.dto.PricingPackageSnapshot;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingPackageFacadeImpl implements PricingPackageFacade {
    private final PricingPackageRepository pricingPackageRepository;
    private final TeacherFacade teacherFacade;

    @Override
    @Transactional(readOnly = true)
    public PricingPackageSnapshot getPurchasablePackage(UUID pricingPackageId) {
        PricingPackage pricingPackage = pricingPackageRepository.findById(pricingPackageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));
        if (pricingPackage.getStatus() != PackageStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PACKAGE_NOT_ACTIVE);
        }
        TeacherSnapshot teacher = teacherFacade.getTeacher(pricingPackage.getTeacherId());
        if (teacher == null
                || !"APPROVED".equalsIgnoreCase(teacher.status())
                || !teacher.isVisible()) {
            throw new BusinessException(ErrorCode.PACKAGE_NOT_ACTIVE);
        }
        return toSnapshot(pricingPackage);
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPackageSnapshot getPackageForPaymentFulfillment(UUID pricingPackageId) {
        PricingPackage pricingPackage = pricingPackageRepository.findById(pricingPackageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));
        return toSnapshot(pricingPackage);
    }

    private PricingPackageSnapshot toSnapshot(PricingPackage pricingPackage) {
        return new PricingPackageSnapshot(
                pricingPackage.getId(), pricingPackage.getTeacherId(), pricingPackage.getSubjectId(),
                pricingPackage.getName(), pricingPackage.getTotalSessions(), pricingPackage.getDurationDays(),
                pricingPackage.getPriceVnd(), pricingPackage.getSessionDurationMinutes(),
                pricingPackage.getStatus().name());
    }
}
