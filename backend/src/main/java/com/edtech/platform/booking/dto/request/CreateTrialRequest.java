package com.edtech.platform.booking.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
public record CreateTrialRequest(@NotNull UUID teacherId, @NotNull UUID subjectId,
        @NotNull Instant preferredStartTime, @Size(max=1000) String note) {}
