package com.edtech.platform.learning.repository;

import com.edtech.platform.learning.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    @org.springframework.data.jpa.repository.Query("select s.assignment.id from Submission s where s.id = :id")
    Optional<UUID> findAssignmentId(UUID id);
    Optional<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);
}
