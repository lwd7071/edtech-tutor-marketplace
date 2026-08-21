package com.edtech.platform.catalog.dto;

import com.edtech.platform.catalog.domain.PackageStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpsertPricingPackageRequest(
        @NotNull(message = "Môn học không được để trống")
        UUID subjectId,

        @NotBlank(message = "Tên gói không được để trống")
        String name,

        String description,

        @Min(value = 1, message = "Tổng số buổi phải lớn hơn 0")
        int totalSessions,

        @Min(value = 1, message = "Thời hạn gói (ngày) phải lớn hơn 0")
        int durationDays,

        @Min(value = 1, message = "Giá gói phải lớn hơn 0")
        long priceVnd,

        @Min(value = 1, message = "Thời lượng mỗi buổi phải lớn hơn 0")
        int sessionDurationMinutes,

        @NotNull(message = "Trạng thái không được để trống")
        PackageStatus status
) {}
