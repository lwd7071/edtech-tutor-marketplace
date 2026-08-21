package com.edtech.platform.teacher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplaceAvailabilityRequest(
        @NotNull(message = "Danh sách lịch rảnh không được null")
        @Valid
        List<AvailabilityItem> availabilities
) {}
