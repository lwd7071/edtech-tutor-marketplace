package com.edtech.platform.learning.domain;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.common.persistence.BaseEntity;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.teacher.domain.TeacherProfile;
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

import java.time.Instant;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "assignment_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AssignmentType assignmentType;

    @Type(JsonType.class)
    @Column(name = "content_blocks", columnDefinition = "jsonb")
    private JsonNode contentBlocks;

    @Type(JsonType.class)
    @Column(name = "quiz_schema", columnDefinition = "jsonb")
    private JsonNode quizSchema;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    @Builder
    public Assignment(TeacherProfile teacher, User student, Subject subject, String title, AssignmentType assignmentType, JsonNode contentBlocks, JsonNode quizSchema, Instant dueAt, AssignmentStatus status) {
        this.teacher = teacher;
        this.student = student;
        this.subject = subject;
        this.title = title;
        this.assignmentType = assignmentType;
        this.contentBlocks = contentBlocks;
        this.quizSchema = quizSchema;
        this.dueAt = dueAt;
        this.status = status;
    }
}
