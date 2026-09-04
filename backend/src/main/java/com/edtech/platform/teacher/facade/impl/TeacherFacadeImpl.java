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
import com.edtech.platform.teacher.repository.TeacherAvailabilityRepository;
import com.edtech.platform.teacher.dto.AvailabilityView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherFacadeImpl implements TeacherFacade {

    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<UUID, TeacherSnapshot> getTeachers(Collection<UUID> teacherIds) {
        var profiles = teacherProfileRepository.findAllById(teacherIds);
        var identities = identityFacade.getIdentities(profiles.stream().map(TeacherProfile::getUserId).filter(java.util.Objects::nonNull).toList());
        return profiles.stream().collect(Collectors.toMap(TeacherProfile::getId, p -> toSnapshot(p, identities)));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<AvailabilityView> getPublicAvailability(UUID teacherId) {
        return teacherAvailabilityRepository.findByTeacherId(teacherId).stream()
                .filter(com.edtech.platform.teacher.domain.TeacherAvailability::isActive)
                .map(a -> new AvailabilityView(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.getTimezone(), a.isActive()))
                .toList();
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
            } catch (IllegalArgumentException e) {
                throw new com.edtech.platform.common.exception.BusinessException(
                        com.edtech.platform.common.exception.ErrorCode.VALIDATION_ERROR);
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
        return toSnapshot(profile, null);
    }

    private TeacherSnapshot toSnapshot(TeacherProfile profile, Map<UUID, IdentitySnapshot> identities) {
        UUID userId = profile.getUserId();
        String fullName = null;
        String avatarUrl = null;
        if (userId != null) {
            java.util.Optional<IdentitySnapshot> identityOpt = identities == null ? identityFacade.getIdentity(userId) : java.util.Optional.ofNullable(identities.get(userId));
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

    @Override
    public boolean isWithinAvailability(UUID teacherId, java.time.Instant start, java.time.Instant end) {
        var availabilities = teacherAvailabilityRepository.findByTeacherId(teacherId).stream()
                .filter(com.edtech.platform.teacher.domain.TeacherAvailability::isActive)
                .toList();
        if (availabilities.isEmpty()) {
            return false;
        }
        for (var a : availabilities) {
            java.time.ZoneId zone = java.time.ZoneId.of(a.getTimezone() != null ? a.getTimezone() : "UTC");
            java.time.ZonedDateTime zStart = start.atZone(zone);
            java.time.ZonedDateTime zEnd = end.atZone(zone);
            if (zStart.getDayOfWeek() == a.getDayOfWeek() && zEnd.getDayOfWeek() == a.getDayOfWeek()) {
                if (!zStart.toLocalTime().isBefore(a.getStartTime()) && !zEnd.toLocalTime().isAfter(a.getEndTime())) {
                    return true;
                }
            }
        }
        return false;
    }
}
