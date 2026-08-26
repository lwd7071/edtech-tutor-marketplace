package com.edtech.platform.teacher.facade;

import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;

import java.util.UUID;

public interface TeacherFacade {
    TeacherSnapshot getTeacher(UUID teacherId);
    TeacherSnapshot getTeacherByUserId(UUID userId);
    boolean hasAssignedSubject(UUID teacherId, UUID subjectId);
    java.util.List<UUID> getApprovedTeacherIds();
    void ensureSubjectAssigned(UUID teacherId, UUID subjectId);
    java.util.Set<UUID> searchTeacherIds(UUID subjectId, String dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime);
    java.util.List<UUID> getSubjectIdsForTeacher(UUID teacherId);
}
