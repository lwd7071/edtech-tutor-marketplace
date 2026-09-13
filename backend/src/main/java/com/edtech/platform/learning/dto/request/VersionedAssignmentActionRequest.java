package com.edtech.platform.learning.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VersionedAssignmentActionRequest(@NotNull @Min(0) Long version) {}
