package com.edtech.platform.enrollment.service;

import com.edtech.platform.enrollment.domain.StudentPackage;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.enrollment.repository.StudentPackageRepository;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class StudentPackageReadService {
    private final StudentPackageRepository packages;
    private final TeacherFacade teachers;
    private final SubjectFacade subjects;

    @Transactional(readOnly=true)
    public Page<StudentOwnedPackageView> list(UUID studentId, StudentPackageStatus status, Pageable pageable) {
        Page<StudentPackage> page = status == null ? packages.findByStudentId(studentId, pageable)
                : packages.findByStudentIdAndStatus(studentId, status, pageable);
        var teacherMap = teachers.getTeachers(page.getContent().stream().map(StudentPackage::getTeacherId).distinct().toList());
        var subjectMap = subjects.getSubjects(page.getContent().stream().map(StudentPackage::getSubjectId).distinct().toList());
        return page.map(p -> map(p, teacherMap.get(p.getTeacherId()), subjectMap.get(p.getSubjectId())));
    }

    @Transactional(readOnly=true)
    public Optional<StudentOwnedPackageView> detail(UUID id, UUID studentId) {
        return packages.findByIdAndStudentId(id, studentId).map(p -> map(p, teachers.getTeacher(p.getTeacherId()), subjects.getSubject(p.getSubjectId())));
    }

    private StudentOwnedPackageView map(StudentPackage p, com.edtech.platform.teacher.facade.dto.TeacherSnapshot t,
                                         com.edtech.platform.subject.facade.dto.SubjectSnapshot s) {
        var teacher = new StudentOwnedPackageView.TeacherRef(p.getTeacherId(), t == null ? "Gia sư" : t.fullName(), t == null ? null : t.avatarUrl());
        var subject = new StudentOwnedPackageView.SubjectRef(p.getSubjectId(), s == null ? "Môn học" : s.name());
        return StudentOwnedPackageView.from(p, teacher, subject);
    }
}
