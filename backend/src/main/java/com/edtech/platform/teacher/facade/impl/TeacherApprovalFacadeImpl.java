package com.edtech.platform.teacher.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.facade.TeacherApprovalFacade;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalChange;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.teacher.repository.TeacherDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.edtech.platform.teacher.facade.dto.TeacherDocumentSnapshot;
import com.edtech.platform.teacher.domain.TeacherDocument;

@Service
@RequiredArgsConstructor
public class TeacherApprovalFacadeImpl implements TeacherApprovalFacade {
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherDocumentRepository teacherDocumentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherApprovalSnapshot> findPendingApprovals(Pageable pageable) {
        return snapshots(teacherProfileRepository.findByProfileStatus(ProfileStatus.PENDING_APPROVAL, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherApprovalSnapshot> findApprovals(String status, Pageable pageable) {
        ProfileStatus parsed;
        try { parsed = status == null ? ProfileStatus.PENDING_APPROVAL : ProfileStatus.valueOf(status); }
        catch (IllegalArgumentException ex) { throw new BusinessException(ErrorCode.VALIDATION_ERROR); }
        return snapshots(teacherProfileRepository.findByProfileStatus(parsed, pageable));
    }

    @Override
    @Transactional
    public TeacherApprovalChange approve(UUID teacherProfileId, UUID adminId) {
        TeacherProfile profile = getProfileForUpdate(teacherProfileId);
        List<TeacherDocument> docs = documents(profile.getId());
        TeacherApprovalSnapshot before = toSnapshot(profile, docs);
        try {
            profile.approve(adminId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.TEACHER_APPROVAL_ALREADY_PROCESSED);
        }
        return new TeacherApprovalChange(before, toSnapshot(profile, docs));
    }

    @Override
    @Transactional
    public TeacherApprovalChange reject(UUID teacherProfileId, UUID adminId, String reason) {
        TeacherProfile profile = getProfileForUpdate(teacherProfileId);
        List<TeacherDocument> docs = documents(profile.getId());
        TeacherApprovalSnapshot before = toSnapshot(profile, docs);
        try {
            profile.reject(reason);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.TEACHER_APPROVAL_ALREADY_PROCESSED);
        }
        return new TeacherApprovalChange(before, toSnapshot(profile, docs));
    }

    private TeacherProfile getProfileForUpdate(UUID teacherProfileId) {
        return teacherProfileRepository.findByIdForUpdate(teacherProfileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
    }

    private Page<TeacherApprovalSnapshot> snapshots(Page<TeacherProfile> page) {
        List<UUID> ids = page.getContent().stream().map(TeacherProfile::getId).toList();
        Map<UUID, List<TeacherDocument>> byTeacher = ids.isEmpty() ? Map.of()
                : teacherDocumentRepository.findByTeacherIdIn(ids).stream()
                .collect(Collectors.groupingBy(document -> document.getTeacher().getId()));
        return page.map(profile -> toSnapshot(profile, byTeacher.getOrDefault(profile.getId(), List.of())));
    }

    private List<TeacherDocument> documents(UUID teacherId) {
        return teacherDocumentRepository.findByTeacherIdIn(List.of(teacherId));
    }

    private TeacherApprovalSnapshot toSnapshot(TeacherProfile profile, List<TeacherDocument> documents) {
        return new TeacherApprovalSnapshot(profile.getId(), profile.getUserId(), profile.getProfileStatus().name(),
                profile.getRejectionReason(), profile.getApprovedById(), profile.getApprovedAt(),
                documents.stream().map(this::documentSnapshot).toList());
    }

    private TeacherDocumentSnapshot documentSnapshot(TeacherDocument document) {
        return new TeacherDocumentSnapshot(document.getId(), document.getDocumentType().name(), document.getTitle(),
                document.getSecureUrl(), document.getMimeType(), document.getFileSize(),
                document.getVerificationStatus().name());
    }
}
