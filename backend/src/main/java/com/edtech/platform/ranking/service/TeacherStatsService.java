package com.edtech.platform.ranking.service;

import org.springframework.stereotype.Service;

import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.common.config.RedisCacheConfig;
import com.edtech.platform.ranking.domain.TeacherStats;
import com.edtech.platform.ranking.dto.response.TeacherRankingItem;
import com.edtech.platform.ranking.dto.response.TeacherStatsView;
import com.edtech.platform.ranking.repository.TeacherStatsRepository;
import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.ranking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;

import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherStatsService {

    private final TeacherStatsRepository teacherStatsRepository;
    private final TeacherFacade teacherFacade;
    private final PlatformSettingsFacade platformSettingsFacade;
    private final BookingEligibilityFacade bookingEligibilityFacade;
    private final ReviewRepository reviewRepository;
    
    private final org.springframework.cache.CacheManager cacheManager;

    @Transactional(readOnly = true)
    public TeacherStatsView getTeacherStats(UUID teacherId) {
        TeacherStats stats = teacherStatsRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_STATS_NOT_FOUND));

        return TeacherStatsView.builder()
                .teacherId(stats.getTeacherId())
                .averageRating(stats.getAverageRating())
                .bayesianRating(stats.getBayesianRating())
                .reviewCount(stats.getReviewCount())
                .completedSessionCount(stats.getCompletedSessionCount())
                .completionRate(stats.getCompletionRate())
                .trialSessionCount(stats.getTrialSessionCount())
                .trialConversionRate(stats.getTrialConversionRate())
                .globalRank(stats.getGlobalRank())
                .calculatedAt(stats.getCalculatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.GLOBAL_RANKING, key = "(#subjectId == null ? 'ALL' : #subjectId.toString()) + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<TeacherRankingItem> getGlobalRanking(UUID subjectId, Pageable pageable) {
        String subjectIdStr = subjectId != null ? subjectId.toString() : null;
        Page<TeacherStats> statsPage = teacherStatsRepository.findGlobalRanking(subjectIdStr, pageable);
        
        Map<UUID, TeacherSnapshot> snapshots = teacherFacade.getTeachers(statsPage.getContent().stream().map(TeacherStats::getTeacherId).toList());
        return statsPage.map(stats -> {
            TeacherSnapshot snapshot = snapshots.get(stats.getTeacherId());

            return TeacherRankingItem.builder()
                    .teacherId(stats.getTeacherId())
                    .fullName(snapshot != null ? snapshot.fullName() : null)
                    .avatarUrl(snapshot != null ? snapshot.avatarUrl() : null)
                    .bioExcerpt(snapshot != null ? snapshot.bioExcerpt() : null)
                    .bayesianRating(stats.getBayesianRating())
                    .completedSessionCount(stats.getCompletedSessionCount())
                    .globalRank(stats.getGlobalRank())
                    .build();
        });
    }

    @Transactional
    public void recalculateTeacherStats(UUID teacherId) {
        log.info("Recalculating TeacherStats for teacher {}", teacherId);
        // 1. Get configuration
        int bayesianMinReviews = platformSettingsFacade.getBayesianMinimumReviews();
        Double globalAverageRating = reviewRepository.findGlobalAverageRating();
        if (globalAverageRating == null) globalAverageRating = 0.0;
        
        double m = bayesianMinReviews;
        double c = globalAverageRating;

        int v = reviewRepository.countVisibleReviewsByTeacherId(teacherId);
        Double rDouble = reviewRepository.findAverageRatingByTeacherId(teacherId);
        double r = rDouble != null ? rDouble : 0.0;

        double bayesianRating = v == 0 ? 0.0 : ((v / (v + m)) * r) + ((m / (v + m)) * c);

        var bookingStats = bookingEligibilityFacade.getTeacherBookingStats(teacherId);
        int completedSessions = bookingStats.completedSessions();
        int totalSessions = bookingStats.totalSessions();
        int trialSessions = bookingStats.trialSessions();
        
        double completionRate = totalSessions > 0 ? (double) completedSessions / totalSessions : 0.0;
        double trialConversionRate = 0.0; 

        TeacherStats stats = teacherStatsRepository.findByTeacherId(teacherId).orElse(TeacherStats.builder().teacherId(teacherId).build());
        stats.setAverageRating(java.math.BigDecimal.valueOf(r).setScale(2, java.math.RoundingMode.HALF_UP));
        stats.setBayesianRating(java.math.BigDecimal.valueOf(bayesianRating).setScale(2, java.math.RoundingMode.HALF_UP));
        stats.setReviewCount(v);
        stats.setCompletedSessionCount(completedSessions);
        stats.setCompletionRate(java.math.BigDecimal.valueOf(completionRate).setScale(4, java.math.RoundingMode.HALF_UP));
        stats.setTrialSessionCount(trialSessions);
        stats.setTrialConversionRate(java.math.BigDecimal.valueOf(trialConversionRate).setScale(4, java.math.RoundingMode.HALF_UP));
        
        teacherStatsRepository.save(stats);

        if (cacheManager.getCache(RedisCacheConfig.GLOBAL_RANKING) != null) {
            cacheManager.getCache(RedisCacheConfig.GLOBAL_RANKING).clear();
        }
        if (cacheManager.getCache(RedisCacheConfig.TEACHER_PUBLIC_PROFILE) != null) {
            cacheManager.getCache(RedisCacheConfig.TEACHER_PUBLIC_PROFILE).evict(teacherId);
        }
        log.info("Recalculation successful for teacher {}", teacherId);
    }
}
