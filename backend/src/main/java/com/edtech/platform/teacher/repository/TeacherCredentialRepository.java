package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.domain.TeacherCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherCredentialRepository extends JpaRepository<TeacherCredential, UUID> {
    List<TeacherCredential> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
    List<TeacherCredential> findByTeacherIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(UUID teacherId, CredentialStatus status);
    long countByTeacherIdAndDeletedFalse(UUID teacherId);
    List<TeacherCredential> findByStatusAndDeletedFalseOrderByCreatedAtAsc(CredentialStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TeacherCredential c where c.id = :id")
    java.util.Optional<TeacherCredential> findByIdForUpdate(@Param("id") UUID id);
}
