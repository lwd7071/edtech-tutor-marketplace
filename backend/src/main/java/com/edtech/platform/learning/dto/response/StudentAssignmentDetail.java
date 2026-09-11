package com.edtech.platform.learning.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;

public record StudentAssignmentDetail(
        @JsonUnwrapped AssignmentDetail assignment,
        SubmissionDetail submission,
        List<com.edtech.platform.common.dto.response.AttachmentView> assignmentAttachments,
        List<com.edtech.platform.common.dto.response.AttachmentView> submissionAttachments) {
}
