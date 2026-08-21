package com.edtech.platform.subject.repository;

import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.SubjectProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubjectProposalRepository extends JpaRepository<SubjectProposal, UUID> {
    Page<SubjectProposal> findByTeacherIdAndStatus(UUID teacherId, ProposalStatus status, Pageable pageable);
    Page<SubjectProposal> findByTeacherId(UUID teacherId, Pageable pageable);
}
