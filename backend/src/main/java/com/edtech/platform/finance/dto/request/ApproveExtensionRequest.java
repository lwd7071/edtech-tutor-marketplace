package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ApproveExtensionRequest(@NotNull Instant approvedExpiryDate, String adminNote) {}
