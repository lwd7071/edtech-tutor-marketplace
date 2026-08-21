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
    Page<Assignment> findByStudentIdAndStatus(UUID studentId, AssignmentStatus status, Pageable pageable);
    Page<Assignment> findByStudentId(UUID studentId, Pageable pageable);
}
