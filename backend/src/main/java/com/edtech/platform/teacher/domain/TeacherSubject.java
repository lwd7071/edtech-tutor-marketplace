package com.edtech.platform.teacher.domain;

import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "teacher_subjects")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE teacher_subjects SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class TeacherSubject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "level_description", columnDefinition = "text")
    private String levelDescription;

    @Column(name = "experience_description", columnDefinition = "text")
    private String experienceDescription;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public TeacherSubject(TeacherProfile teacher, Subject subject, String levelDescription, String experienceDescription) {
        this.teacher = teacher;
        this.subject = subject;
        this.levelDescription = levelDescription;
        this.experienceDescription = experienceDescription;
        this.isActive = true;
    }
}
