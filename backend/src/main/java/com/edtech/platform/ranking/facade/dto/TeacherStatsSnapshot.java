package com.edtech.platform.ranking.facade.dto;

import java.util.UUID;

public record TeacherStatsSnapshot(
    UUID teacherId,
    Double averageRating,
    Double bayesianRating,
    Integer reviewCount,
    Integer completedSessionCount,
    Double completionRate,
    Integer trialSessionCount,
    Double trialConversionRate,
    Integer globalRank
) {}
