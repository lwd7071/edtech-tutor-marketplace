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
}
