package com.edtech.platform.learning.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class GradeSubmissionRequest {
    @NotNull
    @Min(0)
    private Long version;
    @NotNull
    @jakarta.validation.constraints.DecimalMin("0")
    @jakarta.validation.constraints.DecimalMax("10")
    private BigDecimal score;
    
    private String feedbackText;
}
