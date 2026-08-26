package com.edtech.platform.ranking.facade.impl;

import com.edtech.platform.ranking.domain.TeacherStats;
import com.edtech.platform.ranking.facade.TeacherStatsFacade;
import com.edtech.platform.ranking.repository.TeacherStatsRepository;
import com.edtech.platform.ranking.service.TeacherStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherStatsFacadeImpl implements TeacherStatsFacade {

    private final TeacherStatsRepository teacherStatsRepository;
    private final TeacherStatsService teacherStatsService;

    @Override
    public void recalculateTeacherStats(java.util.UUID teacherId) {
        teacherStatsService.recalculateTeacherStats(teacherId);
    }

    @Override
    @Transactional
    public void updateAllGlobalRanks() {
        List<TeacherStats> allStats = new java.util.ArrayList<>(teacherStatsRepository.findAll());
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
        
        log.info("Successfully updated global ranks for {} teachers", allStats.size());
    }

    @Override
    @Transactional(readOnly = true)
    public com.edtech.platform.ranking.facade.dto.TeacherStatsSnapshot getTeacherStats(java.util.UUID teacherId) {
        return teacherStatsRepository.findByTeacherId(teacherId)
                .map(stats -> new com.edtech.platform.ranking.facade.dto.TeacherStatsSnapshot(
                        stats.getTeacherId(),
                        stats.getAverageRating() != null ? stats.getAverageRating().doubleValue() : 0.0,
                        stats.getBayesianRating() != null ? stats.getBayesianRating().doubleValue() : 0.0,
                        stats.getReviewCount(),
                        stats.getCompletedSessionCount(),
                        stats.getCompletionRate() != null ? stats.getCompletionRate().doubleValue() : 0.0,
                        stats.getTrialSessionCount(),
                        stats.getTrialConversionRate() != null ? stats.getTrialConversionRate().doubleValue() : 0.0,
                        stats.getGlobalRank()
                )).orElse(null);
    }
}
