package com.edtech.platform.ranking.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class TeacherRankingItem {
    private UUID teacherId;
    private String fullName;
    private String avatarUrl;
    private String bioExcerpt;
    private BigDecimal bayesianRating;
    private Integer completedSessionCount;
    private Integer globalRank;
}
