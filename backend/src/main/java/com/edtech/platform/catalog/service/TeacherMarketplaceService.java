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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherMarketplaceService {

    private final TeacherFacade teacherFacade;
    private final IdentityFacade identityFacade;
    private final TeacherStatsFacade teacherStatsFacade;
    private final SubjectFacade subjectFacade;
    private final PricingPackageService pricingPackageService;

    public org.springframework.data.domain.Page<TeacherCard> searchTeachers(TeacherSearchParams params) {
        // Find all approved teacher IDs
        List<UUID> teacherIds = new ArrayList<>(teacherFacade.getApprovedTeacherIds());

        // 1. Filter by keyword
        if (params.keyword() != null && !params.keyword().isBlank()) {
            Set<UUID> matchedUserIds = identityFacade.searchUserIdsByKeyword(params.keyword());
            teacherIds = teacherIds.stream()
                .filter(tid -> {
                    TeacherSnapshot ts = teacherFacade.getTeacher(tid);
                    return ts.userId() != null && matchedUserIds.contains(ts.userId());
                }).collect(Collectors.toList());
        }

        // 2. Filter by subject, dayOfWeek, time
        if (params.subjectId() != null || (params.dayOfWeek() != null && params.startTime() != null && params.endTime() != null)) {
            Set<UUID> matchedTeacherIds = teacherFacade.searchTeacherIds(
                params.subjectId(), 
                params.dayOfWeek() != null ? params.dayOfWeek().name() : null, 
                params.startTime(), 
                params.endTime()
            );
            teacherIds.retainAll(matchedTeacherIds);
        }

        // 3. Filter by price
        if (params.minPrice() != null || params.maxPrice() != null) {
            Set<UUID> priceMatchedIds = pricingPackageService.searchTeacherIdsByPrice(params.minPrice(), params.maxPrice());
            teacherIds.retainAll(priceMatchedIds);
        }

        // 4. Map to cards and sort
        List<TeacherCard> cards = new ArrayList<>();
        for (UUID tid : teacherIds) {
            TeacherSnapshot ts = teacherFacade.getTeacher(tid);
            TeacherStatsSnapshot stats = teacherStatsFacade.getTeacherStats(tid);
            
            List<com.edtech.platform.catalog.dto.SubjectDto> subjectDtos = teacherFacade.getSubjectIdsForTeacher(tid).stream()
                .map(sid -> {
                    try {
                        return new com.edtech.platform.catalog.dto.SubjectDto(sid, subjectFacade.getSubject(sid).name());
                    } catch (Exception e) {
                        return new com.edtech.platform.catalog.dto.SubjectDto(sid, "Unknown");
                    }
                })
                .collect(Collectors.toList());

            long minPrice = pricingPackageService.getMinPriceForTeacher(tid);

            cards.add(new TeacherCard(
                ts.id(),
                ts.fullName(),
                ts.avatarUrl(),
                ts.bioExcerpt(),
                ts.yearsOfExperience() != null ? ts.yearsOfExperience() : 0,
                ts.isVerified(),
                ts.supportsOnline(),
                ts.supportsOffline(),
                subjectDtos,
                minPrice,
                stats != null ? stats.averageRating() : 0.0,
                stats != null ? stats.bayesianRating() : 0.0,
                stats != null ? stats.reviewCount() : 0,
                stats != null ? stats.globalRank() : 999999
            ));
        }

        // Sort by rank
        cards.sort(Comparator.comparing(TeacherCard::globalRank, Comparator.nullsLast(Comparator.naturalOrder())));

        // Paginate
        int page = params.page() != null ? params.page() : 0;
        int size = params.size() != null ? params.size() : 20;
        int start = Math.min(page * size, cards.size());
        int end = Math.min((page + 1) * size, cards.size());

        return new org.springframework.data.domain.PageImpl<>(cards.subList(start, end), org.springframework.data.domain.PageRequest.of(page, size), cards.size());
    }

    @Cacheable(value = "TEACHER_PUBLIC_PROFILE", key = "#teacherId")
    public TeacherPublicDetail getTeacherDetail(UUID teacherId) {
        TeacherSnapshot ts = teacherFacade.getTeacher(teacherId);
        if (ts == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        
        TeacherStatsSnapshot stats = teacherStatsFacade.getTeacherStats(teacherId);
        List<String> subjectNames = teacherFacade.getSubjectIdsForTeacher(teacherId).stream()
                .map(sid -> {
                    try {
                        return subjectFacade.getSubject(sid).name();
                    } catch (Exception e) {
                        return "Unknown";
                    }
                })
                .collect(Collectors.toList());

        return new TeacherPublicDetail(
            ts.id(),
            ts.fullName(),
            ts.avatarUrl(),
            ts.bioExcerpt(),
            ts.yearsOfExperience() != null ? ts.yearsOfExperience() : 0,
            ts.languages() != null ? ts.languages() : List.of(),
            ts.supportsOnline(),
            ts.supportsOffline(),
            ts.locationAddress(),
            ts.introductionVideoUrl(),
            subjectNames,
            stats != null ? stats.averageRating() : 0.0,
            stats != null ? stats.bayesianRating() : 0.0,
            stats != null ? stats.reviewCount() : 0,
            stats != null ? stats.globalRank() : 999999
        );
    }
}
