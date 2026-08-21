package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.repository.SubjectRepository;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.domain.TeacherSubject;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.teacher.repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherSubjectService {

    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<TeacherSubjectView> getSubjects(UUID userId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        return teacherSubjectRepository.findByTeacherId(profile.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public TeacherSubjectView assignSubject(UUID userId, UUID subjectId, AssignSubjectRequest request) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));

        Optional<TeacherSubject> existingOpt = teacherSubjectRepository.findByTeacherIdAndSubjectIdIncludingDeleted(profile.getId(), subjectId);

        TeacherSubject teacherSubject;
        if (existingOpt.isPresent()) {
            teacherSubject = existingOpt.get();
            if (!teacherSubject.isDeleted()) {
                throw new BusinessException(ErrorCode.SUBJECT_ALREADY_ASSIGNED);
            }
            teacherSubject.setDeleted(false);
            teacherSubject.setActive(true);
            teacherSubject.setLevelDescription(request.levelDescription());
            teacherSubject.setExperienceDescription(request.experienceDescription());
        } else {
            teacherSubject = TeacherSubject.builder()
                    .teacher(profile)
                    .subject(subject)
                    .levelDescription(request.levelDescription())
                    .experienceDescription(request.experienceDescription())
                    .build();
        }

        return toView(teacherSubjectRepository.save(teacherSubject));
    }

    @Transactional
    public void unassignSubject(UUID userId, UUID subjectId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        // Finding active subjects only
        TeacherSubject teacherSubject = teacherSubjectRepository.findByTeacherId(profile.getId()).stream()
                .filter(ts -> ts.getSubject().getId().equals(subjectId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED));

        teacherSubjectRepository.delete(teacherSubject);
    }

    private TeacherSubjectView toView(TeacherSubject teacherSubject) {
        return new TeacherSubjectView(
                teacherSubject.getId(),
                new TeacherSubjectView.SubjectDto(
                        teacherSubject.getSubject().getId(),
                        teacherSubject.getSubject().getCode(),
                        teacherSubject.getSubject().getName(),
                        teacherSubject.getSubject().getEducationLevel()
                ),
                teacherSubject.getLevelDescription(),
                teacherSubject.getExperienceDescription(),
                teacherSubject.isActive()
        );
    }
}
