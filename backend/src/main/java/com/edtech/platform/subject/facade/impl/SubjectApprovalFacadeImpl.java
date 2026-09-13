package com.edtech.platform.subject.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.domain.SubjectCreatedSource;
import com.edtech.platform.subject.domain.SubjectProposal;
import com.edtech.platform.subject.facade.SubjectApprovalFacade;
import com.edtech.platform.subject.facade.dto.SubjectProposalChange;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import com.edtech.platform.subject.facade.dto.SubjectResolutionCommand;
import com.edtech.platform.subject.repository.SubjectProposalRepository;
import com.edtech.platform.subject.repository.SubjectRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectApprovalFacadeImpl implements SubjectApprovalFacade {
    private final SubjectProposalRepository proposals;
    private final SubjectRepository subjects;
    private final TeacherFacade teachers;

    @Override
    @Transactional(readOnly = true)
    public Page<SubjectProposalSnapshot> findPending(Pageable pageable) {
        return proposals.findByStatus(ProposalStatus.PENDING, pageable).map(this::snapshot);
    }

    @Override
    @Transactional
    public SubjectProposalChange approve(UUID proposalId, UUID adminId, SubjectResolutionCommand command) {
        SubjectProposal proposal = pendingForUpdate(proposalId);
        requireVersion(proposal.getVersion(), command.version());
        SubjectProposalSnapshot before = snapshot(proposal);
        Subject subject = resolveSubject(proposal, command);
        proposal.approve(subject, adminId, command.note());
        teachers.ensureSubjectAssigned(proposal.getTeacherId(), subject.getId());
        return new SubjectProposalChange(before, snapshot(proposal));
    }

    @Override
    @Transactional
    public SubjectProposalChange reject(UUID proposalId, UUID adminId, String reason, long version) {
        SubjectProposal proposal = pendingForUpdate(proposalId);
        requireVersion(proposal.getVersion(), version);
        SubjectProposalSnapshot before = snapshot(proposal);
        proposal.reject(adminId, reason);
        return new SubjectProposalChange(before, snapshot(proposal));
    }

    public SubjectProposalChange reject(UUID proposalId, UUID adminId, String reason) {
        return reject(proposalId, adminId, reason, 0L);
    }

    private SubjectProposal pendingForUpdate(UUID id) {
        SubjectProposal proposal = proposals.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_PROPOSAL_NOT_FOUND));
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new BusinessException(ErrorCode.SUBJECT_PROPOSAL_ALREADY_PROCESSED);
        }
        return proposal;
    }

    private Subject resolveSubject(SubjectProposal proposal, SubjectResolutionCommand command) {
        if (command == null || command.resolution() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (command.resolution() == SubjectResolutionCommand.Resolution.LINK_EXISTING) {
            if (command.existingSubjectId() == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            return subjects.findByIdAndIsActiveTrue(command.existingSubjectId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_INACTIVE));
        }
        if (command.existingSubjectId() != null || command.code() == null || command.code().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String code = command.code().trim().toUpperCase(Locale.ROOT);
        String name = command.name() == null || command.name().isBlank()
                ? proposal.getProposedName() : command.name().trim();
        String slug = slugify(name);
        if (subjects.existsByCodeIgnoreCase(code)) throw new BusinessException(ErrorCode.SUBJECT_CODE_ALREADY_EXISTS);
        if (subjects.existsBySlugIgnoreCase(slug)) throw new BusinessException(ErrorCode.SUBJECT_SLUG_ALREADY_EXISTS);
        EducationLevel level;
        try {
            level = command.educationLevel() == null
                    ? proposal.getEducationLevel() : EducationLevel.valueOf(command.educationLevel());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return subjects.save(Subject.builder().code(code).name(name).slug(slug)
                .educationLevel(level).description(command.description())
                .createdSource(SubjectCreatedSource.TEACHER_PROPOSAL).build());
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        return normalized;
    }

    private SubjectProposalSnapshot snapshot(SubjectProposal p) {
        return new SubjectProposalSnapshot(p.getId(), p.getTeacherId(), p.getProposedName(),
                p.getEducationLevel() == null ? null : p.getEducationLevel().name(), p.getDescription(),
                p.getStatus().name(), p.getReviewNote(), p.getReviewedById(), p.getReviewedAt(),
                p.getCreatedSubject() == null ? null : p.getCreatedSubject().getId(), p.getVersion());
    }

    private void requireVersion(long current, long requested) {
        if (current != requested) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
    }
}
