package com.edtech.platform.teacher.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.teacher.repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherFacadeImpl implements TeacherFacade {

    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final IdentityFacade identityFacade;

    @Override
    public TeacherSnapshot getTeacher(UUID teacherId) {
        TeacherProfile profile = teacherProfileRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        return toSnapshot(profile);
    }

    @Override
    public TeacherSnapshot getTeacherByUserId(UUID userId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        return toSnapshot(profile);
    }

    @Override
    public boolean hasAssignedSubject(UUID teacherId, UUID subjectId) {
        return teacherSubjectRepository.findByTeacherId(teacherId).stream()
                .anyMatch(ts -> ts.getSubjectId().equals(subjectId) && ts.isActive());
    }

    private TeacherSnapshot toSnapshot(TeacherProfile profile) {
        UUID userId = profile.getUserId();
        String fullName = null;
        String avatarUrl = null;
        if (userId != null) {
            try {
                IdentitySnapshot identity = identityFacade.getIdentity(userId).orElseThrow();
                fullName = identity.fullName();
                avatarUrl = identity.avatarUrl();
            } catch (Exception e) {
                // Ignore missing user in snapshot
            }
        }
        String bioExcerpt = profile.getBio() != null ? 
            (profile.getBio().length() > 100 ? profile.getBio().substring(0, 100) + "..." : profile.getBio()) : null;

        return new TeacherSnapshot(
                profile.getId(),
                userId,
                profile.getProfileStatus() != null ? profile.getProfileStatus().name() : null,
                profile.isVerifiedBadge(),
                profile.isVisible(),
                fullName,
                avatarUrl,
                bioExcerpt
        );
    }
}
