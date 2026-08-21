package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/subjects")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService teacherSubjectService;

    @GetMapping
    public ApiResponse<List<TeacherSubjectView>> getSubjects(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(teacherSubjectService.getSubjects(userId));
    }

    @PostMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherSubjectView> assignSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody AssignSubjectRequest request,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.created(teacherSubjectService.assignSubject(userId, subjectId, request));
    }

    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignSubject(@PathVariable UUID subjectId, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        teacherSubjectService.unassignSubject(userId, subjectId);
    }
}
