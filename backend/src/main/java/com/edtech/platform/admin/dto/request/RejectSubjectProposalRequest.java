package com.edtech.platform.admin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RejectSubjectProposalRequest(@NotBlank String reason, @NotNull @Min(0) Long version) {}
