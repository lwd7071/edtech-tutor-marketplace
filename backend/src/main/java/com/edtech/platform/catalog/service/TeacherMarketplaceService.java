package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.ranking.facade.TeacherStatsFacade;
import com.edtech.platform.ranking.facade.dto.TeacherStatsSnapshot;
import com.edtech.platform.subject.facade.SubjectFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import com.edtech.platform.catalog.repository.AdministrativeLocationRepository;

@Service
public class TeacherMarketplaceService {

    private final TeacherFacade teacherFacade;
    private final IdentityFacade identityFacade;
    private final TeacherStatsFacade teacherStatsFacade;
    private final SubjectFacade subjectFacade;
    private final PricingPackageService pricingPackageService;
    private final TeacherSearchCache teacherSearchCache;
    private final TeacherCredentialService credentialService;
    private final AdministrativeLocationRepository locationRepository;

    @Autowired
    public TeacherMarketplaceService(TeacherFacade teacherFacade, IdentityFacade identityFacade,
            TeacherStatsFacade teacherStatsFacade, SubjectFacade subjectFacade,
            PricingPackageService pricingPackageService, TeacherSearchCache teacherSearchCache,
            TeacherCredentialService credentialService, AdministrativeLocationRepository locationRepository) {
        this.teacherFacade = teacherFacade; this.identityFacade = identityFacade;
        this.teacherStatsFacade = teacherStatsFacade; this.subjectFacade = subjectFacade;
        this.pricingPackageService = pricingPackageService; this.teacherSearchCache = teacherSearchCache;
        this.credentialService = credentialService;
        this.locationRepository = locationRepository;
    }

    public TeacherMarketplaceService(TeacherFacade teacherFacade, IdentityFacade identityFacade,
            TeacherStatsFacade teacherStatsFacade, SubjectFacade subjectFacade,
            PricingPackageService pricingPackageService, TeacherSearchCache teacherSearchCache) {
        this(teacherFacade, identityFacade, teacherStatsFacade, subjectFacade, pricingPackageService, teacherSearchCache, null, null);
    }

    public org.springframework.data.domain.Page<TeacherCard> searchTeachers(TeacherSearchParams params) {
        return teacherSearchCache.search(params).toPage();
    }

    @Cacheable(value = "TEACHER_PUBLIC_PROFILE", key = "#teacherId")
    public TeacherPublicDetail getTeacherDetail(UUID teacherId) {
        TeacherSnapshot ts = teacherFacade.getTeacher(teacherId);
        if (ts == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        
        TeacherStatsSnapshot stats = teacherStatsFacade.getTeacherStats(teacherId);
        List<UUID> subjectIds = teacherFacade.getSubjectIdsForTeacher(teacherId);
        Map<UUID, com.edtech.platform.subject.facade.dto.SubjectSnapshot> subjects = subjectFacade.getSubjects(subjectIds);
        List<String> subjectNames = subjectIds.stream().map(subjects::get)
                .filter(Objects::nonNull).map(com.edtech.platform.subject.facade.dto.SubjectSnapshot::name).toList();

        return new TeacherPublicDetail(
            ts.id(),
            ts.fullName(),
            ts.avatarUrl(),
            ts.bioExcerpt(),
            ts.yearsOfExperience() != null ? ts.yearsOfExperience() : 0,
            ts.languages() != null ? ts.languages() : List.of(),
            ts.supportsOnline(),
            ts.supportsOffline(),
            locationRepository == null || ts.provinceCode() == null ? null : locationRepository.findProvinceName(ts.provinceCode()),
            locationRepository == null || ts.wardCode() == null ? null : locationRepository.findWardName(ts.wardCode(), ts.provinceCode()),
            ts.introductionVideoUrl(),
            subjectNames,
            stats != null ? stats.averageRating() : 0.0,
            stats != null ? stats.bayesianRating() : 0.0,
            stats != null ? stats.reviewCount() : 0,
            stats != null ? stats.globalRank() : 999999,
            credentialService == null ? List.of() : credentialService.approvedBadges(teacherId)
        );
    }
}
