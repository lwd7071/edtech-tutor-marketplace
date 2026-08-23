package com.edtech.platform.subject.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.SubjectProposal;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.repository.SubjectProposalRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectProposalService {

    private final SubjectProposalRepository subjectProposalRepository;
    private final TeacherFacade teacherFacade;

    @Transactional
    public SubjectProposalView createProposal(UUID userId, CreateSubjectProposalRequest request) {
        TeacherSnapshot profile = teacherFacade.getTeacherByUserId(userId);

        SubjectProposal proposal = SubjectProposal.builder()
                .teacherId(profile.id())
                .proposedName(request.proposedName())
                .educationLevel(request.educationLevel())
                .description(request.description())
                .build();

        return toView(subjectProposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public Page<SubjectProposalView> getProposals(UUID userId, ProposalStatus status, Pageable pageable) {
        TeacherSnapshot profile = teacherFacade.getTeacherByUserId(userId);

        Page<SubjectProposal> proposals;
        if (status != null) {
            proposals = subjectProposalRepository.findByTeacherIdAndStatus(profile.id(), status, pageable);
        } else {
            proposals = subjectProposalRepository.findByTeacherId(profile.id(), pageable);
        }

        return proposals.map(this::toView);
    }

    @Transactional(readOnly = true)
    public Page<SubjectProposalView> getProposals(UUID userId, String statusStr, Pageable pageable) {
        ProposalStatus status = statusStr != null ? ProposalStatus.valueOf(statusStr) : null;
        return getProposals(userId, status, pageable);
    }

    private SubjectProposalView toView(SubjectProposal proposal) {
        return new SubjectProposalView(
                proposal.getId(),
                proposal.getProposedName(),
                proposal.getEducationLevel(),
                proposal.getDescription(),
                proposal.getStatus(),
                proposal.getReviewNote(),
                proposal.getReviewedAt(),
                proposal.getCreatedSubject() != null ? proposal.getCreatedSubject().getId() : null
        );
    }
}
