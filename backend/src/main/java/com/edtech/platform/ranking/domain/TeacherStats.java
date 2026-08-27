package com.edtech.platform.ranking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.edtech.platform.common.persistence.BaseEntity;

@Entity
@Table(name = "teacher_stats")
@SQLDelete(sql = "UPDATE teacher_stats SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherStats extends BaseEntity {

    @Column(name = "teacher_id", nullable = false, unique = true)
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
    private Instant calculatedAt;
}
