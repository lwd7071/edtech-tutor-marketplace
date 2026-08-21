package com.edtech.platform.subject.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.dto.SubjectSummary;
import com.edtech.platform.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/subjects")
@RequiredArgsConstructor
public class PublicSubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ApiResponse<Page<SubjectSummary>> getPublicSubjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EducationLevel educationLevel,
            Pageable pageable) {
        return ApiResponse.ok(subjectService.getPublicSubjects(keyword, educationLevel, pageable));
    }
}
