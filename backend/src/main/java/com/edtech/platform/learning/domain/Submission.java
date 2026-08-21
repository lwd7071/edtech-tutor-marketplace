package com.edtech.platform.learning.domain;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.common.persistence.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Type(JsonType.class)
    @Column(name = "content_blocks", columnDefinition = "jsonb")
    private JsonNode contentBlocks;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "feedback_text", columnDefinition = "text")
    private String feedbackText;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Builder
    public Submission(Assignment assignment, User student, JsonNode contentBlocks, Instant submittedAt, SubmissionStatus status, BigDecimal score, String feedbackText, Instant gradedAt) {
        this.assignment = assignment;
        this.student = student;
        this.contentBlocks = contentBlocks;
        this.submittedAt = submittedAt;
        this.status = status;
        this.score = score;
        this.feedbackText = feedbackText;
        this.gradedAt = gradedAt;
    }
}
