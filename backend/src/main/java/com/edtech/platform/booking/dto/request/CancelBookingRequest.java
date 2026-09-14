package com.edtech.platform.booking.dto.request;

import com.edtech.platform.booking.domain.CancelInitiatedBy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelBookingRequest(@NotNull @Min(0) Long version,
                                   @NotBlank @Size(max = 1000) String reason,
                                   @NotNull CancelInitiatedBy initiatedBy) {}
