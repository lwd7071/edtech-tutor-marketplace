package com.edtech.platform.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUserStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|LOCKED") String status,
        @NotBlank @Size(max = 1000) String reason) {
}
