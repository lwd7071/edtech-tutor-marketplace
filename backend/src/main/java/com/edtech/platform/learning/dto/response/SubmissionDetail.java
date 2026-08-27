package com.edtech.platform.learning.dto.response;

import com.edtech.platform.learning.domain.SubmissionStatus;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
public class SubmissionDetail {
    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private List<ContentBlock> contentBlocks;
    private Instant submittedAt;
    private SubmissionStatus status;
    private BigDecimal score;
    private String feedbackText;
    private Instant gradedAt;
}
