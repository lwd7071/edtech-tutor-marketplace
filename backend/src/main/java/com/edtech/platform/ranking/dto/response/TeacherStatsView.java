package com.edtech.platform.ranking.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class TeacherStatsView {
    private UUID teacherId;
    private BigDecimal averageRating;
    private BigDecimal bayesianRating;
    private Integer reviewCount;
    private Integer completedSessionCount;
    private BigDecimal completionRate;
    private Integer trialSessionCount;
    private BigDecimal trialConversionRate;
    private Integer globalRank;
    private Instant calculatedAt;
}
