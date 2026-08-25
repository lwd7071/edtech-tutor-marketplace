package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.TeacherDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface TeacherDocumentRepository extends JpaRepository<TeacherDocument, UUID> {
    List<TeacherDocument> findByTeacherIdIn(List<UUID> teacherIds);
}
