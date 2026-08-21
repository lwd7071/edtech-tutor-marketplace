package com.edtech.platform.learning.dto.request;

import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.response.ContentBlock;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateSubmissionRequest {
    private List<ContentBlock> contentBlocks;
    
    @NotNull
    private SubmissionStatus status;
}
