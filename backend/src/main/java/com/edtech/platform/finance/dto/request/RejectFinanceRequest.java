package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectFinanceRequest(@NotBlank @Size(max = 1000) String reason,
                                   @NotNull @Min(0) Long version) {}
