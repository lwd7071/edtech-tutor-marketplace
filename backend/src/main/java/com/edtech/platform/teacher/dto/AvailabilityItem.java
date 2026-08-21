package com.edtech.platform.teacher.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityItem(
        @NotNull(message = "Ngày trong tuần không được để trống")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        LocalTime startTime,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        LocalTime endTime
) {}
