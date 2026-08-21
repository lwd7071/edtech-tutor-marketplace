package com.edtech.platform.learning.dto.response;

import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.AssignmentType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDetail {
    private UUID id;
    private UUID teacherId;
    private UUID studentId;
    private UUID subjectId;
    private String title;
    private AssignmentType assignmentType;
    private List<ContentBlock> contentBlocks;
    private JsonNode quizSchema;
    private Instant dueAt;
    private AssignmentStatus status;
}
