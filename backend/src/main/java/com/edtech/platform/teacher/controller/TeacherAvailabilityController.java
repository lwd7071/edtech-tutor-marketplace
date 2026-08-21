package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.dto.ReplaceAvailabilityRequest;
import com.edtech.platform.teacher.service.TeacherAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/availability")
@RequiredArgsConstructor
public class TeacherAvailabilityController {

    private final TeacherAvailabilityService teacherAvailabilityService;

    @GetMapping
    public ApiResponse<List<AvailabilityView>> getAvailabilities(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(teacherAvailabilityService.getAvailabilities(userId));
    }

    @PutMapping
    public ApiResponse<List<AvailabilityView>> replaceAvailabilities(
            @Valid @RequestBody ReplaceAvailabilityRequest request,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(teacherAvailabilityService.replaceAvailabilities(userId, request));
    }
}
