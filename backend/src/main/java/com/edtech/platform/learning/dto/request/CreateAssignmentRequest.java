package com.edtech.platform.learning.dto.request;

import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.AssignmentType;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CreateAssignmentRequest {
    @NotNull
    private UUID studentId;
    
    @NotNull
    private UUID subjectId;
    
    @NotNull
    private String title;
    
    @NotNull
    private AssignmentType assignmentType;
    
    private List<ContentBlock> contentBlocks;
    
    private JsonNode quizSchema;
    
    private Instant dueAt;
    
    @NotNull
    private AssignmentStatus status;
}
