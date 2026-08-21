package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.TeacherAvailability;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.dto.AvailabilityItem;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.dto.ReplaceAvailabilityRequest;
import com.edtech.platform.teacher.repository.TeacherAvailabilityRepository;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherAvailabilityService {

    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public List<AvailabilityView> getAvailabilities(UUID userId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        return teacherAvailabilityRepository.findByTeacherId(profile.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilityView> getAvailabilitiesByTeacherId(UUID teacherId) {
        return teacherAvailabilityRepository.findByTeacherId(teacherId).stream()
                .filter(TeacherAvailability::isActive) // For public view, only active
                .map(this::toView)
                .toList();
    }

    @Transactional
    public List<AvailabilityView> replaceAvailabilities(UUID userId, ReplaceAvailabilityRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        List<AvailabilityItem> items = request.availabilities();

        for (int i = 0; i < items.size(); i++) {
            AvailabilityItem item1 = items.get(i);
            if (!item1.startTime().isBefore(item1.endTime())) {
                throw new BusinessException(ErrorCode.AVAILABILITY_INVALID_RANGE);
            }

            for (int j = i + 1; j < items.size(); j++) {
                AvailabilityItem item2 = items.get(j);
                if (item1.dayOfWeek() == item2.dayOfWeek()) {
                    if (item1.startTime().isBefore(item2.endTime()) && item1.endTime().isAfter(item2.startTime())) {
                        throw new BusinessException(ErrorCode.AVAILABILITY_TIME_CONFLICT);
                    }
                }
            }
        }

        teacherAvailabilityRepository.deleteByTeacherId(profile.getId());

        List<TeacherAvailability> entitiesToSave = items.stream()
                .map(item -> TeacherAvailability.builder()
                        .teacher(profile)
                        .dayOfWeek(item.dayOfWeek())
                        .startTime(item.startTime())
                        .endTime(item.endTime())
                        .timezone("Asia/Ho_Chi_Minh") 
                        .isActive(true)
                        .build())
                .toList();

        List<TeacherAvailability> saved = teacherAvailabilityRepository.saveAll(entitiesToSave);
        
        if (cacheManager.getCache("TEACHER_PUBLIC_PROFILE") != null) {
            cacheManager.getCache("TEACHER_PUBLIC_PROFILE").evict(profile.getId());
        }
        
        return saved.stream().map(this::toView).toList();
    }

    private AvailabilityView toView(TeacherAvailability ta) {
        return new AvailabilityView(
                ta.getId(),
                ta.getDayOfWeek(),
                ta.getStartTime(),
                ta.getEndTime(),
                ta.getTimezone(),
                ta.isActive()
        );
    }
}
