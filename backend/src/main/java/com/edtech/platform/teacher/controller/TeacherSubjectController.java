package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/subjects")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService teacherSubjectService;

    @GetMapping
    public ApiResponse<List<TeacherSubjectView>> getSubjects(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(teacherSubjectService.getSubjects(user.id()));
    }

    @PostMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherSubjectView> assignSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody AssignSubjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.created(teacherSubjectService.assignSubject(user.id(), subjectId, request));
    }

    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignSubject(@PathVariable UUID subjectId, @AuthenticationPrincipal AuthenticatedUser user) {
        teacherSubjectService.unassignSubject(user.id(), subjectId);
    }
}
