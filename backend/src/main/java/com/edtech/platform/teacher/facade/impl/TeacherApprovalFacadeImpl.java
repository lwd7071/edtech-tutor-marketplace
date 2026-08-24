package com.edtech.platform.teacher.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.facade.TeacherApprovalFacade;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherApprovalFacadeImpl implements TeacherApprovalFacade {
    private final TeacherProfileRepository teacherProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherApprovalSnapshot> findPendingApprovals(Pageable pageable) {
        return teacherProfileRepository.findByProfileStatus(ProfileStatus.PENDING_APPROVAL, pageable)
                .map(this::toSnapshot);
    }

    @Override
    @Transactional
    public TeacherApprovalSnapshot approve(UUID teacherProfileId, UUID adminId) {
        TeacherProfile profile = getProfile(teacherProfileId);
        try {
            profile.approve(adminId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_INVALID_STATE);
        }
        return toSnapshot(profile);
    }

    @Override
    @Transactional
    public TeacherApprovalSnapshot reject(UUID teacherProfileId, String reason) {
        TeacherProfile profile = getProfile(teacherProfileId);
        try {
            profile.reject(reason);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_INVALID_STATE);
        }
        return toSnapshot(profile);
    }

    private TeacherProfile getProfile(UUID teacherProfileId) {
        return teacherProfileRepository.findById(teacherProfileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
    }

    private TeacherApprovalSnapshot toSnapshot(TeacherProfile profile) {
        return new TeacherApprovalSnapshot(profile.getId(), profile.getUserId(), profile.getProfileStatus().name(),
                profile.getRejectionReason(), profile.getApprovedById(), profile.getApprovedAt());
    }
}
