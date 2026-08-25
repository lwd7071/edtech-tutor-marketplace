package com.edtech.platform.subject.facade;

import com.edtech.platform.subject.facade.dto.SubjectProposalChange;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import com.edtech.platform.subject.facade.dto.SubjectResolutionCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubjectApprovalFacade {
    Page<SubjectProposalSnapshot> findPending(Pageable pageable);
    SubjectProposalChange approve(UUID proposalId, UUID adminId, SubjectResolutionCommand command);
    SubjectProposalChange reject(UUID proposalId, UUID adminId, String reason);
}
