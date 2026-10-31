package com.edtech.platform.ranking.service;

import com.edtech.platform.common.config.RedisCacheConfig;
import com.edtech.platform.ranking.domain.TeacherStats;
import com.edtech.platform.ranking.repository.TeacherStatsRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherRankingCache {
    private final TeacherStatsRepository teacherStatsRepository;
    private final TeacherFacade teacherFacade;

    @Cacheable(
            value = RedisCacheConfig.GLOBAL_RANKING,
            key = "(#p0 == null ? 'ALL' : #p0.toString()) + '_' + #p1 + '_' + #p2"
    )
    public CachedTeacherRankingPage get(UUID subjectId, int page, int size) {
        String subjectIdString = subjectId == null ? null : subjectId.toString();
        Page<TeacherStats> statsPage = teacherStatsRepository.findGlobalRanking(
                subjectIdString, PageRequest.of(page, size));
        Map<UUID, TeacherSnapshot> snapshots = teacherFacade.getTeachers(
                statsPage.getContent().stream().map(TeacherStats::getTeacherId).toList());

        List<CachedTeacherRankingItem> content = statsPage.getContent().stream()
                .map(stats -> {
                    TeacherSnapshot snapshot = snapshots.get(stats.getTeacherId());
                    return new CachedTeacherRankingItem(
                            stats.getTeacherId(),
                            snapshot == null ? null : snapshot.fullName(),
                            snapshot == null ? null : snapshot.avatarUrl(),
                            snapshot == null ? null : snapshot.bioExcerpt(),
                            stats.getBayesianRating(),
                            stats.getCompletedSessionCount(),
                            stats.getGlobalRank());
                })
                .toList();

        return new CachedTeacherRankingPage(content, statsPage.getNumber(), statsPage.getSize(), statsPage.getTotalElements());
    }
}

record CachedTeacherRankingPage(
        List<CachedTeacherRankingItem> content,
        int page,
        int size,
        long totalElements) {
    CachedTeacherRankingPage {
        content = List.copyOf(content);
    }

    Page<com.edtech.platform.ranking.dto.response.TeacherRankingItem> toPage(org.springframework.data.domain.Sort sort) {
        var items = content.stream().map(CachedTeacherRankingItem::toTeacherRankingItem).toList();
        return new org.springframework.data.domain.PageImpl<>(
                items, PageRequest.of(page, size, sort), totalElements);
    }
}

record CachedTeacherRankingItem(
        UUID teacherId,
        String fullName,
        String avatarUrl,
        String bioExcerpt,
        BigDecimal bayesianRating,
        Integer completedSessionCount,
        Integer globalRank) {
    com.edtech.platform.ranking.dto.response.TeacherRankingItem toTeacherRankingItem() {
        return com.edtech.platform.ranking.dto.response.TeacherRankingItem.builder()
                .teacherId(teacherId)
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .bioExcerpt(bioExcerpt)
                .bayesianRating(bayesianRating)
                .completedSessionCount(completedSessionCount)
                .globalRank(globalRank)
                .build();
    }
}
