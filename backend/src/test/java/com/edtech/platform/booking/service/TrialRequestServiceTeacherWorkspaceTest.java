package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.booking.domain.TrialRequest;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.TrialRequestRepository;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialRequestServiceTeacherWorkspaceTest {
    @Mock private TrialRequestRepository requests;
    @Mock private BookingRepository bookings;
    @Mock private TeacherFacade teachers;
    @Mock private IdentityFacade identities;
    @Mock private CommunicationFacade communications;
    @Mock private SubjectFacade subjects;
    @InjectMocks private TrialRequestService service;

    @Test
    void workspacePageBatchLoadsLearnerAndSubjectNamesForOwnedTeacherRequests() {
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(new TeacherSnapshot(teacherProfileId, teacherUserId,
                "APPROVED", true, true, "Teacher", null, null, 1, true, false, List.of("vi"), null, null));
        TrialRequest request = TrialRequest.create(studentId, teacherProfileId, subjectId,
                Instant.parse("2026-10-30T12:00:00Z"), "Mục tiêu học thử");
        var pageable = PageRequest.of(0, 12, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(requests.findByTeacherId(teacherProfileId, pageable)).thenReturn(new PageImpl<>(List.of(request), pageable, 1));
        when(identities.getIdentities(List.of(studentId))).thenReturn(Map.of(studentId,
                new IdentitySnapshot(studentId, "student@example.test", "Nguyễn An", "STUDENT", "ACTIVE", null, false, null)));
        when(subjects.getSubjects(List.of(subjectId))).thenReturn(Map.of(subjectId,
                new SubjectSnapshot(subjectId, "ALG", "Đại số", "HIGH_SCHOOL", true)));

        var page = service.findTeacherWorkspace(teacherUserId, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().studentName()).isEqualTo("Nguyễn An");
        assertThat(page.getContent().getFirst().subjectName()).isEqualTo("Đại số");
        assertThat(page.getContent().getFirst().request().status()).isEqualTo(TrialRequestStatus.PENDING);
        verify(identities).getIdentities(List.of(studentId));
        verify(subjects).getSubjects(List.of(subjectId));
        verify(identities, never()).getIdentity(studentId);
    }

    @Test
    void pendingWorkspaceUsesPendingStatusQueryAndBatchLabels() {
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(new TeacherSnapshot(teacherProfileId, teacherUserId,
                "APPROVED", true, true, "Teacher", null, null, 1, true, false, List.of("vi"), null, null));
        TrialRequest request = TrialRequest.create(studentId, teacherProfileId, subjectId,
                Instant.parse("2026-10-30T12:00:00Z"), "Mục tiêu học thử");
        var pageable = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(requests.findByTeacherIdAndStatus(teacherProfileId, TrialRequestStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(request), pageable, 1));
        when(identities.getIdentities(List.of(studentId))).thenReturn(Map.of(studentId,
                new IdentitySnapshot(studentId, "student@example.test", "Nguyễn An", "STUDENT", "ACTIVE", null, false, null)));
        when(subjects.getSubjects(List.of(subjectId))).thenReturn(Map.of(subjectId,
                new SubjectSnapshot(subjectId, "ALG", "Đại số", "HIGH_SCHOOL", true)));

        var page = service.findPendingTeacherWorkspace(teacherUserId, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().request().status()).isEqualTo(TrialRequestStatus.PENDING);
        verify(requests).findByTeacherIdAndStatus(teacherProfileId, TrialRequestStatus.PENDING, pageable);
        verify(identities).getIdentities(List.of(studentId));
        verify(subjects).getSubjects(List.of(subjectId));
        verify(identities, never()).getIdentity(studentId);
    }
}
