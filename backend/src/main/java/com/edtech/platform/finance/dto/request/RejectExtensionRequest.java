package com.edtech.platform.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectExtensionRequest(@NotBlank @Size(max = 1000) String reason) {}
