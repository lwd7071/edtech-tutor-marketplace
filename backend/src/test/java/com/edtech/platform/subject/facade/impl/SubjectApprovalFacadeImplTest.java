package com.edtech.platform.subject.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.SubjectProposal;
import com.edtech.platform.subject.repository.SubjectProposalRepository;
import com.edtech.platform.subject.repository.SubjectRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectApprovalFacadeImplTest {
    @Mock SubjectProposalRepository proposals;
    @Mock SubjectRepository subjects;
    @Mock TeacherFacade teachers;

    @Test
    void resolvingAnAlreadyReviewedProposalReturnsConflictCode() {
        UUID id = UUID.randomUUID();
        SubjectProposal proposal = SubjectProposal.builder()
                .teacherId(UUID.randomUUID()).proposedName("Vật lý lượng tử").build();
        ReflectionTestUtils.setField(proposal, "id", id);
        ReflectionTestUtils.setField(proposal, "status", ProposalStatus.APPROVED);
        when(proposals.findByIdForUpdate(id)).thenReturn(Optional.of(proposal));
        SubjectApprovalFacadeImpl facade = new SubjectApprovalFacadeImpl(proposals, subjects, teachers);

        assertThatThrownBy(() -> facade.reject(id, UUID.randomUUID(), "Trùng môn"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUBJECT_PROPOSAL_ALREADY_PROCESSED));
    }
}
