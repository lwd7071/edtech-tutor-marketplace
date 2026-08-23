package com.edtech.platform.teacher.facade;

import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;

import java.util.UUID;

public interface TeacherFacade {
    TeacherSnapshot getTeacher(UUID teacherId);
    TeacherSnapshot getTeacherByUserId(UUID userId);
    boolean hasAssignedSubject(UUID teacherId, UUID subjectId);
}
