package com.edtech.platform.scheduler;

import com.edtech.platform.common.config.RedisCacheConfig;
import com.edtech.platform.ranking.facade.TeacherStatsFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.edtech.platform.teacher.facade.TeacherFacade;

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

    private final TeacherStatsFacade teacherStatsFacade;
    private final CacheManager cacheManager;
    private final TeacherFacade teacherFacade;

    @Scheduled(cron = "0 0 2 * * ?") // 02:00 every day
    @SchedulerLock(name = "TeacherStatsJob_calculateStats", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void calculateTeacherStats() {
        log.info("Starting TeacherStatsJob");
        // 2. Fetch all approved teachers
        List<UUID> teacherIds = teacherFacade.getApprovedTeacherIds();

        int successCount = 0;
        int failCount = 0;

        // 3. Process each teacher
        for (UUID teacherId : teacherIds) {
            teacherStatsFacade.recalculateTeacherStats(teacherId);
            successCount++;
        }

        // 4. Update global rank
        teacherStatsFacade.updateAllGlobalRanks();

        log.info("TeacherStatsJob completed. Processed {} records.", successCount);

        // 5. Invalidate Cache
        if (cacheManager.getCache(RedisCacheConfig.GLOBAL_RANKING) != null) {
            cacheManager.getCache(RedisCacheConfig.GLOBAL_RANKING).clear();
        }
    }
}
