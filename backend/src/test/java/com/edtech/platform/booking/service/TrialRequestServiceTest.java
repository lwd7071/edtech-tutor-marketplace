package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.domain.TrialRequest;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.TrialRequestRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialRequestServiceTest {

    @Mock private TrialRequestRepository trialRequestRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TeacherFacade teacherFacade;
    @Mock private IdentityFacade identityFacade;
    @Mock private CommunicationFacade communicationFacade;

    private TrialRequestService trialRequestService;

    private final UUID studentUserId = UUID.randomUUID();
    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        trialRequestService = new TrialRequestService(
                trialRequestRepository,
                bookingRepository,
                teacherFacade,
                identityFacade,
                communicationFacade
        );
    }

    private TeacherSnapshot mockTeacherSnapshot() {
        return new TeacherSnapshot(
                teacherId, teacherUserId, "APPROVED", true, true,
                "Teacher Name", "avatar.png", "Bio", 5, true, false,
                List.of("VIETNAMESE"), "Address", "video.mp4"
        );
    }

    @Test
    void create_shouldSucceed_whenNoPending() {
        CreateTrialRequest req = new CreateTrialRequest(
                teacherId, subjectId, Instant.now().plusSeconds(7200), "I want a trial"
        );

        when(identityFacade.isActive(studentUserId)).thenReturn(true);
        when(teacherFacade.getTeacher(teacherId)).thenReturn(mockTeacherSnapshot());
        when(teacherFacade.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(trialRequestRepository.existsByTeacherIdAndStudentIdAndStatus(teacherId, studentUserId, TrialRequestStatus.PENDING))
                .thenReturn(false);
        when(trialRequestRepository.saveAndFlush(any(TrialRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrialRequest result = trialRequestService.create(studentUserId, req);

        assertThat(result.getStudentId()).isEqualTo(studentUserId);
        assertThat(result.getTeacherId()).isEqualTo(teacherId);
        assertThat(result.getStatus()).isEqualTo(TrialRequestStatus.PENDING);
        verify(communicationFacade).publishAfterCommit(any());
    }

    @Test
    void create_shouldThrow_whenPendingAlreadyExists() {
        CreateTrialRequest req = new CreateTrialRequest(
                teacherId, subjectId, Instant.now().plusSeconds(7200), "I want a trial"
        );

        when(identityFacade.isActive(studentUserId)).thenReturn(true);
        when(teacherFacade.getTeacher(teacherId)).thenReturn(mockTeacherSnapshot());
        when(teacherFacade.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(trialRequestRepository.existsByTeacherIdAndStudentIdAndStatus(teacherId, studentUserId, TrialRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> trialRequestService.create(studentUserId, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.TRIAL_REQUEST_ALREADY_PENDING));
    }

    @Test
    void accept_shouldCreateTrialBooking_andTransitionStatus() {
        UUID requestId = UUID.randomUUID();
        TrialRequest req = TrialRequest.create(studentUserId, teacherId, subjectId, Instant.now().plusSeconds(7200), "Note");

        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        AcceptTrialRequest acceptReq = new AcceptTrialRequest(start, end, DeliveryMode.ONLINE, "https://meet.google.com/abc", null);

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        when(trialRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(req));
        when(bookingRepository.existsOverlapTeacher(teacherId, start, end)).thenReturn(false);
        when(bookingRepository.existsOverlapStudent(studentUserId, start, end)).thenReturn(false);
        when(teacherFacade.isWithinAvailability(teacherId, start, end)).thenReturn(true);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDetail detail = trialRequestService.accept(teacherUserId, requestId, acceptReq);

        assertThat(detail.trial()).isTrue();
        assertThat(detail.studentPackageId()).isNull();
        assertThat(req.getStatus()).isEqualTo(TrialRequestStatus.ACCEPTED);
        verify(communicationFacade).publishAfterCommit(any());
    }

    @Test
    void reject_shouldTransitionStatus_withoutBooking() {
        UUID requestId = UUID.randomUUID();
        TrialRequest req = TrialRequest.create(studentUserId, teacherId, subjectId, Instant.now().plusSeconds(7200), "Note");

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        when(trialRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(req));

        TrialRequest rejected = trialRequestService.reject(teacherUserId, requestId, "Teacher busy");

        assertThat(rejected.getStatus()).isEqualTo(TrialRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Teacher busy");
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(communicationFacade).publishAfterCommit(any());
    }
}
