package com.edtech.platform.subject.repository;

import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.SubjectProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SubjectProposalRepository extends JpaRepository<SubjectProposal, UUID> {
    Page<SubjectProposal> findByTeacherIdAndStatus(UUID teacherId, ProposalStatus status, Pageable pageable);
    Page<SubjectProposal> findByTeacherId(UUID teacherId, Pageable pageable);
    Page<SubjectProposal> findByStatus(ProposalStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SubjectProposal p where p.id = :id")
    Optional<SubjectProposal> findByIdForUpdate(@Param("id") UUID id);
}
