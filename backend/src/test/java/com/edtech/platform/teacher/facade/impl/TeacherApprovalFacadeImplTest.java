package com.edtech.platform.teacher.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherDocumentRepository;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
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
class TeacherApprovalFacadeImplTest {
    @Mock TeacherProfileRepository profiles;
    @Mock TeacherDocumentRepository documents;

    @Test
    void approvingAnAlreadyProcessedProfileReturnsConflictCode() {
        UUID id = UUID.randomUUID();
        TeacherProfile profile = TeacherProfile.builder().userId(UUID.randomUUID()).build();
        ReflectionTestUtils.setField(profile, "id", id);
        ReflectionTestUtils.setField(profile, "profileStatus", ProfileStatus.APPROVED);
        when(profiles.findByIdForUpdate(id)).thenReturn(Optional.of(profile));
        TeacherApprovalFacadeImpl facade = new TeacherApprovalFacadeImpl(profiles, documents);

        assertThatThrownBy(() -> facade.approve(id, UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TEACHER_APPROVAL_ALREADY_PROCESSED));
    }
}
