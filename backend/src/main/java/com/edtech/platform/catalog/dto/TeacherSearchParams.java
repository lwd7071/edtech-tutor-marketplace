package com.edtech.platform.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record TeacherSearchParams(
        String keyword,
        UUID subjectId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        @Min(value = 0, message = "Giá tối thiểu phải lớn hơn hoặc bằng 0") Long minPrice,
        @Min(value = 0, message = "Giá tối đa phải lớn hơn hoặc bằng 0") Long maxPrice,
        @DecimalMin(value = "0.0", message = "Đánh giá tối thiểu phải >= 0")
        @DecimalMax(value = "5.0", message = "Đánh giá tối thiểu phải <= 5") Double minRating,
        @Pattern(regexp = "(?i)^(ONLINE|OFFLINE)$", message = "deliveryMode phải là ONLINE hoặc OFFLINE") String deliveryMode,
        @Pattern(regexp = "(?i)^(price_asc|price_desc|rating_desc|experience_desc)$", message = "sort phải là price_asc, price_desc, rating_desc hoặc experience_desc") String sort,
        @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0") Integer page,
        @Min(value = 1, message = "Kích thước trang phải lớn hơn hoặc bằng 1")
        @Max(value = 100, message = "Kích thước trang không được vượt quá 100") Integer size
) {
    @AssertTrue(message = "Giá tối thiểu phải nhỏ hơn hoặc bằng giá tối đa")
    public boolean isValidPriceRange() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice <= maxPrice;
    }

    @AssertTrue(message = "Phải truyền đủ dayOfWeek, startTime, endTime khi lọc theo giờ")
    public boolean isValidTimeFilter() {
        int count = 0;
        if (dayOfWeek != null) count++;
        if (startTime != null) count++;
        if (endTime != null) count++;
        return count == 0 || count == 3;
    }
}
