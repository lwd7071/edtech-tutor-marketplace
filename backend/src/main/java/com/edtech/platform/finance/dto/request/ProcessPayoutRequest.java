package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProcessPayoutRequest(@NotNull @Min(0) Long version) {}
