package com.edtech.platform.ranking.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.config.RedisCacheConfig;
import com.edtech.platform.ranking.domain.TeacherStats;
import com.edtech.platform.ranking.dto.response.TeacherRankingItem;
import com.edtech.platform.ranking.dto.response.TeacherStatsView;
import com.edtech.platform.ranking.repository.TeacherStatsRepository;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherStatsService {

    private final TeacherStatsRepository teacherStatsRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final org.springframework.cache.CacheManager cacheManager;

    @Transactional(readOnly = true)
    public TeacherStatsView getTeacherStats(UUID teacherId) {
        TeacherStats stats = teacherStatsRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TEACHER_STATS_NOT_FOUND"));

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
        
        return statsPage.map(stats -> {
            TeacherProfile profile = teacherProfileRepository.findById(stats.getTeacherId()).orElse(null);
            User user = null;
            if (profile != null) {
                user = profile.getUser();
            }

            String bioExcerpt = profile != null && profile.getBio() != null ?
                    (profile.getBio().length() > 100 ? profile.getBio().substring(0, 100) + "..." : profile.getBio()) : null;

            return TeacherRankingItem.builder()
                    .teacherId(stats.getTeacherId())
                    .fullName(user != null ? user.getFullName() : null)
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .bioExcerpt(bioExcerpt)
                    .bayesianRating(stats.getBayesianRating())
                    .completedSessionCount(stats.getCompletedSessionCount())
                    .globalRank(stats.getGlobalRank())
                    .build();
        });
    }

    @Transactional
    public void recalculateTeacherStats(UUID teacherId) {
        log.info("Recalculating TeacherStats for teacher {}", teacherId);
        try {
            // 1. Get configuration
            Integer bayesianMinReviews = jdbcTemplate.queryForObject(
                    "SELECT bayesian_minimum_reviews FROM platform_settings LIMIT 1", Integer.class);
            if (bayesianMinReviews == null) bayesianMinReviews = 10;
            
            Double globalAverageRating = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE is_visible = true AND is_deleted = false", Double.class);
            if (globalAverageRating == null) globalAverageRating = 0.0;
            
            double m = bayesianMinReviews;
            double c = globalAverageRating;

            Map<String, Object> reviewStats = jdbcTemplate.queryForMap(
                    "SELECT COUNT(id) as review_count, COALESCE(AVG(rating), 0) as average_rating " +
                    "FROM reviews WHERE teacher_id = ? AND is_visible = true AND is_deleted = false", teacherId);
            
            int v = ((Number) reviewStats.get("review_count")).intValue();
            double r = ((Number) reviewStats.get("average_rating")).doubleValue();

            double bayesianRating = v == 0 ? 0.0 : ((v / (v + m)) * r) + ((m / (v + m)) * c);

            Map<String, Object> bookingStats = jdbcTemplate.queryForMap(
                    "SELECT " +
                    "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_sessions, " +
                    "COUNT(CASE WHEN status IN ('COMPLETED', 'CANCELLED', 'SCHEDULED') THEN 1 END) as total_sessions, " +
                    "COUNT(CASE WHEN is_trial = true AND status = 'COMPLETED' THEN 1 END) as trial_sessions " +
                    "FROM bookings WHERE teacher_id = ? AND is_deleted = false", teacherId);
            
            int completedSessions = ((Number) bookingStats.get("completed_sessions")).intValue();
            int totalSessions = ((Number) bookingStats.get("total_sessions")).intValue();
            int trialSessions = ((Number) bookingStats.get("trial_sessions")).intValue();
            
            double completionRate = totalSessions > 0 ? (double) completedSessions / totalSessions : 0.0;
            double trialConversionRate = 0.0; 

            TeacherStats stats = teacherStatsRepository.findById(teacherId).orElse(TeacherStats.builder().teacherId(teacherId).build());
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
        } catch (Exception e) {
            log.error("Failed to recalculate stats for teacher {}", teacherId, e);
        }
    }
}
