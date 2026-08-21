package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, UUID> {

    List<TeacherSubject> findByTeacherId(UUID teacherId);

    @Query(value = "SELECT * FROM teacher_subjects WHERE teacher_id = :teacherId AND subject_id = :subjectId", nativeQuery = true)
    Optional<TeacherSubject> findByTeacherIdAndSubjectIdIncludingDeleted(@Param("teacherId") UUID teacherId, @Param("subjectId") UUID subjectId);
}
