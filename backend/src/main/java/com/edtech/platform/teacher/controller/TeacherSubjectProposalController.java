package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.service.SubjectProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/subject-proposals")
@RequiredArgsConstructor
public class TeacherSubjectProposalController {

    private final SubjectProposalService subjectProposalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectProposalView> createProposal(
            @Valid @RequestBody CreateSubjectProposalRequest request,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.created(subjectProposalService.createProposal(userId, request));
    }

    @GetMapping
    public ApiResponse<Page<SubjectProposalView>> getProposals(
            @RequestParam(required = false) ProposalStatus status,
            Pageable pageable,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(subjectProposalService.getProposals(userId, status, pageable));
    }
}
