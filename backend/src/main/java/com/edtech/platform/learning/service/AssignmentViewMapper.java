package com.edtech.platform.learning.service;

import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AssignmentViewMapper {
    private final ObjectMapper objectMapper;

    public AssignmentDetail assignment(Assignment value) {
        List<ContentBlock> blocks = value.getContentBlocks() == null ? null
                : objectMapper.convertValue(value.getContentBlocks(), new TypeReference<>() {});
        return new AssignmentDetail(value.getId(), value.getTeacherId(), value.getStudentId(), value.getSubjectId(),
                value.getTitle(), value.getAssignmentType(), blocks, value.getQuizSchema(), value.getDueAt(), value.getStatus(), value.getVersion());
    }

    public SubmissionDetail submission(Submission value) {
        List<ContentBlock> blocks = value.getContentBlocks() == null ? null
                : objectMapper.convertValue(value.getContentBlocks(), new TypeReference<>() {});
        return new SubmissionDetail(value.getId(), value.getAssignment().getId(), value.getStudentId(), blocks,
                value.getSubmittedAt(), value.getStatus(), value.getScore(), value.getFeedbackText(), value.getGradedAt(), value.getVersion());
    }
}
