package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.catalog.dto.ChangePackageStatusRequest;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.UpsertPricingPackageRequest;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingPackageService {

    private final PricingPackageRepository pricingPackageRepository;
    private final TeacherFacade teacherFacade;
    private final SubjectFacade subjectFacade;
    private final EnrollmentFacade enrollmentFacade;
    private final CacheManager cacheManager;

    @Transactional
    public PricingPackageView createPackage(UUID userId, UpsertPricingPackageRequest request) {
        TeacherSnapshot profile = requireApprovedTeacher(userId);
        if (request.version() != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Version khi tạo gói phải bằng 0");
        }

        SubjectSnapshot subject = subjectFacade.getSubject(request.subjectId());

        boolean hasSubject = teacherFacade.hasAssignedSubject(profile.id(), subject.id());

        if (!hasSubject) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }
        
        PricingPackage pkg = PricingPackage.builder()
                .teacherId(profile.id())
                .subjectId(subject.id())
                .name(request.name())
                .description(request.description())
                .totalSessions(request.totalSessions())
                .durationDays(request.durationDays())
                .priceVnd(request.priceVnd())
                .sessionDurationMinutes(request.sessionDurationMinutes())
                .status(request.status())
                .build();

        PricingPackage saved = pricingPackageRepository.save(pkg);
        evictTeacherProfileCache(profile.id());
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public Page<PricingPackageView> getOwnPackages(UUID userId, Pageable pageable) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(userId);
        if (teacher == null) throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        return pricingPackageRepository.findByTeacherId(teacher.id(), pageable).map(this::toView);
    }

    @Transactional(readOnly = true)
    public PricingPackageView getOwnPackage(UUID userId, UUID id) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(userId);
        PricingPackage pkg = pricingPackageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));
        if (teacher == null || !pkg.getTeacherId().equals(teacher.id()))
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return toView(pkg);
    }

    @Transactional
    public PricingPackageView updatePackage(UUID userId, UUID packageId, UpsertPricingPackageRequest request) {
        TeacherSnapshot profile = requireApprovedTeacher(userId);

        PricingPackage pkg = pricingPackageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));

        if (!pkg.getTeacherId().equals(profile.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        requireCurrentVersion(pkg, request.version());

        if (enrollmentFacade.hasStudentPackage(packageId)) {
            throw new BusinessException(ErrorCode.PACKAGE_IMMUTABLE_AFTER_PURCHASE);
        }

        SubjectSnapshot subject = subjectFacade.getSubject(request.subjectId());

        boolean hasSubject = teacherFacade.hasAssignedSubject(profile.id(), subject.id());

        if (!hasSubject) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }

        pkg.setSubjectId(subject.id());
        pkg.setName(request.name());
        pkg.setDescription(request.description());
        pkg.setTotalSessions(request.totalSessions());
        pkg.setDurationDays(request.durationDays());
        pkg.setPriceVnd(request.priceVnd());
        pkg.setSessionDurationMinutes(request.sessionDurationMinutes());
        pkg.setStatus(request.status());

        PricingPackage saved = pricingPackageRepository.save(pkg);
        evictTeacherProfileCache(profile.id());
        return toView(saved);
    }

    @Transactional
    public PricingPackageView changeStatus(UUID userId, UUID packageId, ChangePackageStatusRequest request) {
        TeacherSnapshot profile = requireTeacher(userId);

        PricingPackage pkg = pricingPackageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));

        if (!pkg.getTeacherId().equals(profile.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        requireCurrentVersion(pkg, request.version());

        if (request.status() == PackageStatus.ACTIVE) {
            requireApproved(profile);
        }

        if (enrollmentFacade.hasStudentPackage(packageId)) {
            if (request.status() != PackageStatus.INACTIVE) {
                throw new BusinessException(ErrorCode.PACKAGE_INVALID_STATE);
            }
        }

        pkg.setStatus(request.status());

        PricingPackage saved = pricingPackageRepository.save(pkg);
        evictTeacherProfileCache(profile.id());
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public Page<PricingPackageView> getTeacherPackages(UUID teacherId, Pageable pageable) {
        return pricingPackageRepository.findByTeacherIdAndStatus(teacherId, PackageStatus.ACTIVE, pageable)
                .map(this::toView);
    }

    private PricingPackageView toView(PricingPackage pkg) {
        String subjectName = subjectFacade.getSubject(pkg.getSubjectId()).name();

        return new PricingPackageView(
                pkg.getId(),
                pkg.getSubjectId(),
                subjectName,
                pkg.getName(),
                pkg.getDescription(),
                pkg.getTotalSessions(),
                pkg.getDurationDays(),
                pkg.getPriceVnd(),
                pkg.getSessionDurationMinutes(),
                pkg.getStatus(),
                pkg.getVersion()
        );
    }

    private void evictTeacherProfileCache(UUID teacherId) {
        if (cacheManager.getCache("TEACHER_PUBLIC_PROFILE") != null) {
            cacheManager.getCache("TEACHER_PUBLIC_PROFILE").evict(teacherId);
        }
    }

    private TeacherSnapshot requireTeacher(UUID userId) {
        TeacherSnapshot profile = teacherFacade.getTeacherByUserId(userId);
        if (profile == null) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        }
        return profile;
    }

    private TeacherSnapshot requireApprovedTeacher(UUID userId) {
        TeacherSnapshot profile = requireTeacher(userId);
        requireApproved(profile);
        return profile;
    }

    private void requireApproved(TeacherSnapshot profile) {
        if (!"APPROVED".equalsIgnoreCase(profile.status())) {
            throw new BusinessException(ErrorCode.TEACHER_NOT_APPROVED);
        }
    }

    private void requireCurrentVersion(PricingPackage pkg, long requestedVersion) {
        if (pkg.getVersion() != requestedVersion) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    @Transactional(readOnly = true)
    public java.util.Set<UUID> searchTeacherIdsByPrice(Long minPrice, Long maxPrice) {
        return pricingPackageRepository.searchTeacherIdsByPrice(minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public Long getMinPriceForTeacher(UUID teacherId) {
        return pricingPackageRepository.findByTeacherIdAndStatus(teacherId, PackageStatus.ACTIVE, PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("priceVnd").ascending()))
                .stream()
                .findFirst()
                .map(PricingPackage::getPriceVnd)
                .orElse(0L);
    }
}
