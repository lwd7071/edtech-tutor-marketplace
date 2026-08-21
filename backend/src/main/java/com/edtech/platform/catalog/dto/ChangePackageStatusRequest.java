package com.edtech.platform.catalog.dto;

import com.edtech.platform.catalog.domain.PackageStatus;
import jakarta.validation.constraints.NotNull;

public record ChangePackageStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        PackageStatus status
) {}
