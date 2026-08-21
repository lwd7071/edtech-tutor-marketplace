package com.edtech.platform.subject.domain;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.common.persistence.BaseEntity;
import com.edtech.platform.teacher.domain.TeacherProfile;
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
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "subject_proposals")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE subject_proposals SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class SubjectProposal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_subject_id")
    private Subject createdSubject;

    @Builder
    public SubjectProposal(TeacherProfile teacher, String proposedName, EducationLevel educationLevel, String description) {
        this.teacher = teacher;
        this.proposedName = proposedName;
        this.educationLevel = educationLevel;
        this.description = description;
        this.status = ProposalStatus.PENDING;
    }
}
