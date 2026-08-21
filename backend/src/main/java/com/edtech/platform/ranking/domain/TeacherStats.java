package com.edtech.platform.ranking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherStats {

    @Id
    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "bayesian_rating", nullable = false)
    @Builder.Default
    private BigDecimal bayesianRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "completed_session_count", nullable = false)
    @Builder.Default
    private Integer completedSessionCount = 0;

    @Column(name = "completion_rate", nullable = false)
    @Builder.Default
    private BigDecimal completionRate = BigDecimal.ZERO;

    @Column(name = "trial_session_count", nullable = false)
    @Builder.Default
    private Integer trialSessionCount = 0;

    @Column(name = "trial_conversion_rate", nullable = false)
    @Builder.Default
    private BigDecimal trialConversionRate = BigDecimal.ZERO;

    @Column(name = "global_rank")
    private Integer globalRank;

    @UpdateTimestamp
    @Column(name = "calculated_at", nullable = false)
    private ZonedDateTime calculatedAt;
}
