package com.edtech.platform.learning.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@SQLDelete(sql = "UPDATE assignments SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

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
    public Assignment(UUID teacherId, UUID studentId, UUID subjectId, String title, AssignmentType assignmentType, JsonNode contentBlocks, JsonNode quizSchema, Instant dueAt, AssignmentStatus status) {
        this.teacherId = teacherId;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.title = title;
        this.assignmentType = assignmentType;
        this.contentBlocks = contentBlocks;
        this.quizSchema = quizSchema;
        this.dueAt = dueAt;
        this.status = status;
    }
}
