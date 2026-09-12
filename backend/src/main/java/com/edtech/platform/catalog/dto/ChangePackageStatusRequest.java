package com.edtech.platform.catalog.dto;

import com.edtech.platform.catalog.domain.PackageStatus;
import jakarta.validation.constraints.NotNull;

public record ChangePackageStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        PackageStatus status,
        @jakarta.validation.constraints.Min(value = 0, message = "Version không hợp lệ")
        long version
) {
    public ChangePackageStatusRequest(PackageStatus status) {
        this(status, 0L);
    }
}
