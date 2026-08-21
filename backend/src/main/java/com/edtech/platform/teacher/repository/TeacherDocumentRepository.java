package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.TeacherDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeacherDocumentRepository extends JpaRepository<TeacherDocument, UUID> {
}
