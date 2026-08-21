package com.edtech.platform.subject.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.SubjectProposal;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.repository.SubjectProposalRepository;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
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
    private final TeacherProfileRepository teacherProfileRepository;

    @Transactional
    public SubjectProposalView createProposal(UUID userId, CreateSubjectProposalRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        SubjectProposal proposal = SubjectProposal.builder()
                .teacher(profile)
                .proposedName(request.proposedName())
                .educationLevel(request.educationLevel())
                .description(request.description())
                .build();

        return toView(subjectProposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public Page<SubjectProposalView> getProposals(UUID userId, ProposalStatus status, Pageable pageable) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        Page<SubjectProposal> proposals;
        if (status != null) {
            proposals = subjectProposalRepository.findByTeacherIdAndStatus(profile.getId(), status, pageable);
        } else {
            proposals = subjectProposalRepository.findByTeacherId(profile.getId(), pageable);
        }

        return proposals.map(this::toView);
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
