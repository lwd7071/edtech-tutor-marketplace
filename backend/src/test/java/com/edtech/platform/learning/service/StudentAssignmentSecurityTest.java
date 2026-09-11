package com.edtech.platform.learning.service;

import com.edtech.platform.learning.domain.*;
import com.edtech.platform.learning.dto.request.CreateSubmissionRequest;
import com.edtech.platform.learning.repository.*;
import com.edtech.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;

class StudentAssignmentSecurityTest {
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final SubmissionRepository submissions = mock(SubmissionRepository.class);
    private final StudentAssignmentService service = new StudentAssignmentService(
            assignments, submissions, null, null, null, null, null, null);

    @Test void draftFilterNeverQueriesPrivateDrafts() {
        assertTrue(service.getAssignments(UUID.randomUUID(), AssignmentStatus.DRAFT, Pageable.unpaged()).isEmpty());
        verifyNoInteractions(assignments);
    }

    @Test void studentCannotAssignGradedStatus() {
        var request = new CreateSubmissionRequest();
        request.setStatus(SubmissionStatus.GRADED);
        assertThrows(BusinessException.class, () -> service.createOrUpdateSubmission(UUID.randomUUID(), UUID.randomUUID(), request));
        verifyNoInteractions(assignments, submissions);
    }
}
