package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.dto.UpdateTeacherProfileRequest;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherProfileService {

    private final TeacherProfileRepository teacherProfileRepository;

    @Transactional(readOnly = true)
    public TeacherProfileDetail getProfile(UUID userId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        return toDetail(profile);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.id"),
        @CacheEvict(value = "GLOBAL_RANKING", allEntries = true)
    })
    public TeacherProfileDetail updateProfile(UUID userId, UpdateTeacherProfileRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        if (profile.getProfileStatus() == ProfileStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_INVALID_STATE);
        }

        if (profile.getProfileStatus() == ProfileStatus.APPROVED) {
            profile.setProfileStatus(ProfileStatus.DRAFT);
        }

        profile.setBio(request.bio());
        profile.setYearsOfExperience(request.yearsOfExperience());
        profile.setLanguages(request.languages());
        profile.setSupportsOnline(request.supportsOnline());
        profile.setSupportsOffline(request.supportsOffline());
        profile.setLocationAddress(request.locationAddress());
        profile.setIntroductionVideoUrl(request.introductionVideoUrl());

        return toDetail(teacherProfileRepository.save(profile));
    }

    @Transactional
    @CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.id")
    public TeacherProfileDetail submitProfile(UUID userId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        try {
            profile.submitForApproval();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_INVALID_STATE);
        }

        return toDetail(teacherProfileRepository.save(profile));
    }

    private TeacherProfileDetail toDetail(TeacherProfile profile) {
        return new TeacherProfileDetail(
                profile.getId(),
                profile.getBio(),
                profile.getYearsOfExperience(),
                profile.getLanguages(),
                profile.isSupportsOnline(),
                profile.isSupportsOffline(),
                profile.getLocationAddress(),
                profile.getIntroductionVideoUrl(),
                profile.getProfileStatus(),
                profile.getRejectionReason(),
                profile.isVerifiedBadge(),
                profile.isVisible()
        );
    }
}
