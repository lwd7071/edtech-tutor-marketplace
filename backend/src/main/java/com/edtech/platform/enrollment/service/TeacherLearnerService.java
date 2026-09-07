package com.edtech.platform.enrollment.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.enrollment.repository.StudentPackageRepository;
import com.edtech.platform.enrollment.dto.StudentPackageDetail;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherLearnerService {
    private final StudentPackageRepository packages;
    private final TeacherFacade teachers;
    private final IdentityFacade identities;
    private final SubjectFacade subjects;

    public record LearnerPackage(UUID studentId, String studentName, String subjectName, StudentPackageDetail studentPackage) {}

    @Transactional(readOnly = true)
    public Page<LearnerPackage> list(UUID teacherUserId, UUID studentId, Pageable pageable) {
        var teacher = teachers.getTeacherByUserId(teacherUserId);
        if (teacher == null) throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        var rows = studentId == null ? packages.findByTeacherId(teacher.id(), pageable)
                : packages.findByTeacherIdAndStudentId(teacher.id(), studentId, pageable);
        var names = identities.getIdentities(rows.stream().map(p -> p.getStudentId()).distinct().toList());
        var subjectNames = subjects.getSubjects(rows.stream().map(p -> p.getSubjectId()).distinct().toList());
        return rows.map(p -> {
            var identity = names.get(p.getStudentId());
            var subject = subjectNames.get(p.getSubjectId());
            return new LearnerPackage(p.getStudentId(), identity == null ? "Học viên" : identity.fullName(),
                    subject == null ? "Môn học" : subject.name(), StudentPackageDetail.from(p));
        });
    }
}

