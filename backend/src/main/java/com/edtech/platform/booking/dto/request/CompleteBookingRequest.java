package com.edtech.platform.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompleteBookingRequest(@NotNull @Min(0) Long version, @NotNull @Valid SessionReportRequest report) {}
