package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.dto.request.*;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.dto.response.SessionReportView;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.facade.dto.BookingPackageSnapshot;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.SessionReportRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private EnrollmentBookingFacade enrollmentBookingFacade;
    @Mock private SessionReportRepository sessionReportRepository;
    @Mock private FinanceFacade financeFacade;
    @Mock private CommunicationFacade communicationFacade;
    @Mock private TeacherFacade teacherFacade;
    @Mock private IdentityFacade identityFacade;

    private BookingService bookingService;

    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                enrollmentBookingFacade,
                sessionReportRepository,
                financeFacade,
                communicationFacade,
                teacherFacade,
                identityFacade
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
    void create_shouldSucceed_whenValid() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        CreateBookingRequest req = new CreateBookingRequest(
                packageId, start, end, DeliveryMode.ONLINE, "https://meet.google.com/abc", null
        );

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        BookingPackageSnapshot pkg = new BookingPackageSnapshot(
                packageId, studentId, teacherId, subjectId, "ACTIVE",
                10, 0, 0, 0, 10, 1000000L, new BigDecimal("5.00"),
                Instant.now().plusSeconds(86400 * 30), 0
        );
        when(enrollmentBookingFacade.inspect(packageId, null)).thenReturn(pkg);
        when(identityFacade.isActive(studentId)).thenReturn(true);
        when(teacherFacade.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(bookingRepository.existsOverlapTeacher(teacherId, start, end)).thenReturn(false);
        when(bookingRepository.existsOverlapStudent(studentId, start, end)).thenReturn(false);
        when(teacherFacade.isWithinAvailability(teacherId, start, end)).thenReturn(true);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDetail result = bookingService.create(teacherUserId, req);

        assertThat(result.teacherId()).isEqualTo(teacherId);
        assertThat(result.studentId()).isEqualTo(studentId);
        assertThat(result.status()).isEqualTo(BookingStatus.SCHEDULED);
        assertThat(result.outsideAvailabilityWarning()).isFalse();

        verify(enrollmentBookingFacade).reserveSession(packageId);
        verify(communicationFacade).publishAfterCommit(any());
    }

    @Test
    void create_shouldThrow_whenTimeInPast() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now().plusSeconds(3600);
        CreateBookingRequest req = new CreateBookingRequest(
                packageId, start, end, DeliveryMode.ONLINE, null, null
        );

        assertThatThrownBy(() -> bookingService.create(teacherUserId, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_IN_PAST));
    }

    @Test
    void create_shouldThrow_whenTeacherOverlap() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        CreateBookingRequest req = new CreateBookingRequest(
                packageId, start, end, DeliveryMode.ONLINE, null, null
        );

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        BookingPackageSnapshot pkg = new BookingPackageSnapshot(
                packageId, studentId, teacherId, subjectId, "ACTIVE",
                10, 0, 0, 0, 10, 1000000L, new BigDecimal("5.00"),
                Instant.now().plusSeconds(86400 * 30), 0
        );
        when(enrollmentBookingFacade.inspect(packageId, null)).thenReturn(pkg);
        when(identityFacade.isActive(studentId)).thenReturn(true);
        when(teacherFacade.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(bookingRepository.existsOverlapTeacher(teacherId, start, end)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(teacherUserId, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_TIME_CONFLICT));
    }

    @Test
    void complete_shouldSucceed_andSettlePendingToAvailable_whenOfficial() {
        UUID bookingId = UUID.randomUUID();
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now().minusSeconds(3600);
        Booking booking = spy(Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false));

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(sessionReportRepository.existsByBookingId(bookingId)).thenReturn(false);

        BookingPackageSnapshot pkg = new BookingPackageSnapshot(
                packageId, studentId, teacherId, subjectId, "ACTIVE",
                9, 1, 0, 0, 10, 1000000L, new BigDecimal("5.00"),
                Instant.now().plusSeconds(86400 * 30), 0
        );
        when(enrollmentBookingFacade.inspect(packageId, studentId)).thenReturn(pkg);

        CompleteBookingRequest req = new CompleteBookingRequest(
                0L, new SessionReportRequest("https://rec.example.com", "Content", "Feedback", "Note", 5)
        );

        BookingDetail detail = bookingService.complete(teacherUserId, bookingId, req);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(booking.isSettlementProcessed()).isTrue();

        verify(sessionReportRepository).save(any(SessionReport.class));
        verify(enrollmentBookingFacade).completeReservedSession(packageId);

        // Teacher Net Total = 1,000,000 - 5% (50,000) = 950,000. Session Net = floor(1 * 950,000 / 10) = 95,000
        verify(financeFacade).settleBookingSession(eq(teacherId), eq(bookingId), eq(95000L));
        verify(communicationFacade).publishAfterCommit(any());
    }

    @Test
    void complete_shouldThrow_whenBeforeEndTime() {
        UUID bookingId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(100);
        Instant end = Instant.now().plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        CompleteBookingRequest req = new CompleteBookingRequest(
                0L, new SessionReportRequest(null, "Content", null, null, 5)
        );

        assertThatThrownBy(() -> bookingService.complete(teacherUserId, bookingId, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_NOT_ENDED));
    }

    @Test
    void cancel_shouldSucceed_andReleaseSession() {
        UUID bookingId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Booking booking = Booking.scheduleOfficial(teacherId, studentId, packageId, subjectId, start, end, DeliveryMode.ONLINE, null, null, false);

        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        CancelBookingRequest req = new CancelBookingRequest(0L, "Student sick", CancelInitiatedBy.TEACHER);

        bookingService.cancel(teacherUserId, bookingId, req);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo("Student sick");
        verify(enrollmentBookingFacade).releaseReservedSession(packageId);
        verify(communicationFacade).publishAfterCommit(any());
    }

    @Test
    void findStudentSessionReports_shouldReturnPagedReports() {
        UUID bookingId = UUID.randomUUID();
        SessionReport report = SessionReport.create(
                bookingId, "https://rec.example.com", "Content", "Feedback", "Note", 5, Instant.now()
        );
        ReflectionTestUtils.setField(report, "id", UUID.randomUUID());
        Booking booking = Booking.scheduleOfficial(
                teacherId, studentId, packageId, subjectId,
                Instant.now().minusSeconds(3600), Instant.now(),
                DeliveryMode.ONLINE, null, null, false
        );
        ReflectionTestUtils.setField(booking, "id", bookingId);

        when(sessionReportRepository.findByStudentId(eq(studentId), any()))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(bookingRepository.findAllById(List.of(bookingId)))
                .thenReturn(List.of(booking));

        Page<SessionReportView> result = bookingService.findStudentSessionReports(studentId, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        SessionReportView view = result.getContent().get(0);
        assertThat(view.bookingId()).isEqualTo(bookingId);
        assertThat(view.teacherId()).isEqualTo(teacherId);
        assertThat(view.subjectId()).isEqualTo(subjectId);
        assertThat(view.content()).isEqualTo("Content");
    }
}