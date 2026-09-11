package com.edtech.platform.learning.repository;

import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Assignment a where a.id = :id")
    java.util.Optional<Assignment> findByIdForUpdate(UUID id);
    Page<Assignment> findByStudentIdAndStatusIn(UUID studentId, java.util.Collection<AssignmentStatus> statuses, Pageable pageable);
    Page<Assignment> findByTeacherId(UUID teacherId, Pageable pageable);
    Page<Assignment> findByStudentIdAndStatus(UUID studentId, AssignmentStatus status, Pageable pageable);
    Page<Assignment> findByStudentId(UUID studentId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        select a from Assignment a where a.studentId=:studentId and a.status=:published and
        not exists (select s.id from Submission s where s.assignment=a and s.studentId=:studentId and s.status in (:submittedStatuses))
        """)
    Page<Assignment> findStudentTodo(UUID studentId, AssignmentStatus published,
        java.util.Collection<com.edtech.platform.learning.domain.SubmissionStatus> submittedStatuses, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        select a from Assignment a where a.studentId=:studentId and
        exists (select s.id from Submission s where s.assignment=a and s.studentId=:studentId and s.status=:submissionStatus)
        """)
    Page<Assignment> findStudentBySubmissionStatus(UUID studentId,
        com.edtech.platform.learning.domain.SubmissionStatus submissionStatus, Pageable pageable);
}
