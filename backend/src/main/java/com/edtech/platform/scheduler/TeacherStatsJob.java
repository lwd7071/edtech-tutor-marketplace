package com.edtech.platform.scheduler;

import com.edtech.platform.common.config.RedisCacheConfig;
import com.edtech.platform.ranking.domain.TeacherStats;
import com.edtech.platform.ranking.repository.TeacherStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherStatsJob {

    private final JdbcTemplate jdbcTemplate;
    private final TeacherStatsRepository teacherStatsRepository;
    private final CacheManager cacheManager;
    private final com.edtech.platform.ranking.service.TeacherStatsService teacherStatsService;

    @Scheduled(cron = "0 0 2 * * ?") // 02:00 every day
    @SchedulerLock(name = "TeacherStatsJob_calculateStats", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void calculateTeacherStats() {
        log.info("Starting TeacherStatsJob");
        try {
            // 2. Fetch all approved teachers
            List<UUID> teacherIds = jdbcTemplate.queryForList(
                    "SELECT id FROM teacher_profiles WHERE profile_status = 'APPROVED' AND is_deleted = false", UUID.class);

            int successCount = 0;
            int failCount = 0;

            // 3. Process each teacher
            for (UUID teacherId : teacherIds) {
                try {
                    teacherStatsService.recalculateTeacherStats(teacherId);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to calculate stats for teacher {}", teacherId, e);
                    failCount++;
                }
            }

            // 4. Update global rank
            List<TeacherStats> allStats = teacherStatsRepository.findAll();
            allStats.sort((s1, s2) -> {
                int cmp = s2.getBayesianRating().compareTo(s1.getBayesianRating());
                if (cmp != 0) return cmp;
                cmp = s2.getCompletedSessionCount().compareTo(s1.getCompletedSessionCount());
                if (cmp != 0) return cmp;
                return s2.getCompletionRate().compareTo(s1.getCompletionRate());
            });

            int rank = 1;
            for (TeacherStats stat : allStats) {
                stat.setGlobalRank(rank++);
            }
            teacherStatsRepository.saveAll(allStats);

            log.info("TeacherStatsJob completed. Processed {} records, {} failed.", successCount, failCount);

            // 5. Invalidate Cache
            if (cacheManager.getCache(RedisCacheConfig.GLOBAL_RANKING) != null) {
                cacheManager.getCache(RedisCacheConfig.GLOBAL_RANKING).clear();
            }

        } catch (Exception ex) {
            log.error("TeacherStatsJob failed", ex);
        }
    }
}
