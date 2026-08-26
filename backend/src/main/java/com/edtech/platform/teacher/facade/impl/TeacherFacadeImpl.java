package com.edtech.platform.teacher.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.domain.TeacherSubject;
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

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void ensureSubjectAssigned(UUID teacherId, UUID subjectId) {
        TeacherProfile profile = teacherProfileRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        var existing = teacherSubjectRepository.findByTeacherIdAndSubjectIdIncludingDeleted(teacherId, subjectId);
        if (existing.isPresent()) {
            TeacherSubject assignment = existing.get();
            assignment.setDeleted(false);
            assignment.setActive(true);
            teacherSubjectRepository.save(assignment);
            return;
        }
        teacherSubjectRepository.save(TeacherSubject.builder().teacher(profile).subjectId(subjectId).build());
    }
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

    @Override
    public java.util.List<UUID> getApprovedTeacherIds() {
        return teacherProfileRepository.findApprovedTeacherIds();
    }

    @Override
    public java.util.Set<UUID> searchTeacherIds(UUID subjectId, String dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime) {
        java.time.DayOfWeek day = null;
        if (dayOfWeek != null) {
            try {
                day = java.time.DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            } catch (Exception e) {
                // Invalid day, ignore filter or log
            }
        }
        return teacherProfileRepository.searchTeacherIds(subjectId, day, startTime, endTime);
    }

    @Override
    public java.util.List<UUID> getSubjectIdsForTeacher(UUID teacherId) {
        return teacherSubjectRepository.findByTeacherId(teacherId).stream()
                .filter(ts -> ts.isActive() && !ts.isDeleted())
                .map(com.edtech.platform.teacher.domain.TeacherSubject::getSubjectId)
                .collect(java.util.stream.Collectors.toList());
    }

    private TeacherSnapshot toSnapshot(TeacherProfile profile) {
        UUID userId = profile.getUserId();
        String fullName = null;
        String avatarUrl = null;
        if (userId != null) {
            java.util.Optional<IdentitySnapshot> identityOpt = identityFacade.getIdentity(userId);
            if (identityOpt.isPresent()) {
                fullName = identityOpt.get().fullName();
                avatarUrl = identityOpt.get().avatarUrl();
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
                bioExcerpt,
                profile.getYearsOfExperience(),
                profile.isSupportsOnline(),
                profile.isSupportsOffline(),
                profile.getLanguages() != null ? profile.getLanguages() : java.util.List.of(),
                profile.getLocationAddress(),
                profile.getIntroductionVideoUrl()
        );
    }
}
