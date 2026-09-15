package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.dto.UpdateTeacherProfileRequest;
import com.edtech.platform.teacher.dto.UpdateTeacherResidenceRequest;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.admin.facade.AuditTrailFacade;
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
    private final AuditTrailFacade auditTrail;
    private final com.edtech.platform.common.cache.PublicCacheRevalidationClient revalidationClient;

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
        profile.setIntroductionVideoUrl(request.introductionVideoUrl());

        return toDetail(teacherProfileRepository.save(profile));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.id"),
        @CacheEvict(value = "GLOBAL_RANKING", allEntries = true),
        @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)
    })
    public TeacherProfileDetail updateResidence(UUID userId, UpdateTeacherResidenceRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        String beforeProvince = profile.getProvinceCode();
        String beforeWard = profile.getWardCode();
        if (java.util.Objects.equals(beforeProvince, request.provinceCode())
                && java.util.Objects.equals(beforeWard, request.wardCode())) {
            return toDetail(profile);
        }
        profile.setProvinceCode(request.provinceCode());
        profile.setWardCode(request.wardCode());
        TeacherProfile saved = teacherProfileRepository.save(profile);
        java.util.Map<String, Object> before = new java.util.HashMap<>();
        before.put("provinceCode", beforeProvince); before.put("wardCode", beforeWard);
        java.util.Map<String, Object> after = new java.util.HashMap<>();
        after.put("provinceCode", saved.getProvinceCode()); after.put("wardCode", saved.getWardCode());
        auditTrail.append(userId, "TEACHER_RESIDENCE_UPDATED", "TEACHER_PROFILE", saved.getId(), before, after);
        if (revalidationClient != null) {
            revalidationClient.revalidateTeacherPublicData(saved.getId(), "TEACHER_RESIDENCE_UPDATED");
        }
        return toDetail(saved);
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
                profile.getProvinceCode(),
                profile.getWardCode(),
                profile.getIntroductionVideoUrl(),
                profile.getProfileStatus(),
                profile.getRejectionReason(),
                profile.isVerifiedBadge(),
                profile.isVisible()
        );
    }
}
