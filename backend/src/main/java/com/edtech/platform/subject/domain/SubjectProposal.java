package com.edtech.platform.subject.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import java.util.UUID;

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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "subject_proposals")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE subject_proposals SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class SubjectProposal extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "proposed_name", nullable = false, length = 150)
    private String proposedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 50)
    private EducationLevel educationLevel;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "reviewed_by")
    private UUID reviewedById;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_subject_id")
    private Subject createdSubject;

    @jakarta.persistence.Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    @Builder
    public SubjectProposal(UUID teacherId, String proposedName, EducationLevel educationLevel, String description) {
        this.teacherId = teacherId;
        this.proposedName = proposedName;
        this.educationLevel = educationLevel;
        this.description = description;
        this.status = ProposalStatus.PENDING;
    }

    public void approve(Subject subject, UUID adminId, String note) {
        requirePending();
        this.status = ProposalStatus.APPROVED;
        this.createdSubject = subject;
        this.reviewedById = adminId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void reject(UUID adminId, String reason) {
        requirePending();
        this.status = ProposalStatus.REJECTED;
        this.reviewedById = adminId;
        this.reviewNote = reason;
        this.reviewedAt = Instant.now();
    }

    private void requirePending() {
        if (status != ProposalStatus.PENDING) {
            throw new IllegalStateException("Proposal has already been reviewed");
        }
    }
}
