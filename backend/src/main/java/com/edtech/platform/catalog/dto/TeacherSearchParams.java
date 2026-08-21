package com.edtech.platform.catalog.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record TeacherSearchParams(
        String keyword,
        UUID subjectId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Long minPrice,
        Long maxPrice,
        Integer page,
        Integer size
) {}
