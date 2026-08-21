package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.catalog.dto.ChangePackageStatusRequest;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.UpsertPricingPackageRequest;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.repository.SubjectRepository;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.teacher.repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingPackageService {

    private final PricingPackageRepository pricingPackageRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final StudentPackageChecker studentPackageChecker;
    private final CacheManager cacheManager;

    @Transactional
    public PricingPackageView createPackage(UUID userId, UpsertPricingPackageRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        if (profile.getProfileStatus() != ProfileStatus.APPROVED) {
            throw new BusinessException(ErrorCode.TEACHER_NOT_APPROVED);
        }

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));

        boolean hasSubject = teacherSubjectRepository.findByTeacherId(profile.getId()).stream()
                .anyMatch(ts -> ts.getSubject().getId().equals(subject.getId()) && ts.isActive());

        if (!hasSubject) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }

        PricingPackage pkg = PricingPackage.builder()
                .teacher(profile)
                .subject(subject)
                .name(request.name())
                .description(request.description())
                .totalSessions(request.totalSessions())
                .durationDays(request.durationDays())
                .priceVnd(request.priceVnd())
                .sessionDurationMinutes(request.sessionDurationMinutes())
                .status(request.status())
                .build();

        PricingPackage saved = pricingPackageRepository.save(pkg);
        evictTeacherProfileCache(profile.getId());
        return toView(saved);
    }

    @Transactional
    public PricingPackageView updatePackage(UUID userId, UUID packageId, UpsertPricingPackageRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        PricingPackage pkg = pricingPackageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));

        if (!pkg.getTeacher().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (studentPackageChecker.hasStudentPackage(packageId)) {
            throw new BusinessException(ErrorCode.PACKAGE_IMMUTABLE_AFTER_PURCHASE);
        }

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));

        boolean hasSubject = teacherSubjectRepository.findByTeacherId(profile.getId()).stream()
                .anyMatch(ts -> ts.getSubject().getId().equals(subject.getId()) && ts.isActive());

        if (!hasSubject) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }

        pkg.setSubject(subject);
        pkg.setName(request.name());
        pkg.setDescription(request.description());
        pkg.setTotalSessions(request.totalSessions());
        pkg.setDurationDays(request.durationDays());
        pkg.setPriceVnd(request.priceVnd());
        pkg.setSessionDurationMinutes(request.sessionDurationMinutes());
        pkg.setStatus(request.status());

        PricingPackage saved = pricingPackageRepository.save(pkg);
        evictTeacherProfileCache(profile.getId());
        return toView(saved);
    }

    @Transactional
    public PricingPackageView changeStatus(UUID userId, UUID packageId, ChangePackageStatusRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        PricingPackage pkg = pricingPackageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));

        if (!pkg.getTeacher().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (studentPackageChecker.hasStudentPackage(packageId)) {
            if (request.status() != PackageStatus.INACTIVE) {
                throw new BusinessException(ErrorCode.PACKAGE_INVALID_STATE);
            }
        }

        pkg.setStatus(request.status());

        PricingPackage saved = pricingPackageRepository.save(pkg);
        evictTeacherProfileCache(profile.getId());
        return toView(saved);
    }

    private PricingPackageView toView(PricingPackage pkg) {
        return new PricingPackageView(
                pkg.getId(),
                pkg.getSubject().getId(),
                pkg.getSubject().getName(),
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
}
