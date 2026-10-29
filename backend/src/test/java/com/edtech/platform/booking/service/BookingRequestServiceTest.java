package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.domain.BookingRequest;
import com.edtech.platform.booking.domain.BookingRequestStatus;
import com.edtech.platform.booking.dto.request.CreateBookingRequestCommand;
import com.edtech.platform.booking.dto.request.CreateBookingRequest;
import com.edtech.platform.booking.dto.request.AcceptBookingRequestCommand;
import com.edtech.platform.booking.dto.request.RejectBookingRequestCommand;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.response.BookingRequestView;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.facade.dto.BookingPackageSnapshot;
import com.edtech.platform.booking.repository.BookingRequestRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingRequestServiceTest {
    @Mock private BookingRequestRepository requests;
    @Mock private EnrollmentBookingFacade packages;
    @Mock private TeacherFacade teachers;
    @Mock private IdentityFacade identities;
    @Mock private BookingService bookings;
    @Mock private CommunicationFacade communication;
    @Mock private SubjectFacade subjects;

    private final Instant now = Instant.parse("2026-10-29T00:00:00Z");
    private final java.util.UUID studentId = java.util.UUID.randomUUID();
    private final java.util.UUID teacherId = java.util.UUID.randomUUID();
    private final java.util.UUID teacherUserId = java.util.UUID.randomUUID();
    private final java.util.UUID subjectId = java.util.UUID.randomUUID();
    private final java.util.UUID packageId = java.util.UUID.randomUUID();
    private BookingRequestService service;

    @BeforeEach
    void setUp() {
        service = new BookingRequestService(requests, packages, teachers, identities, bookings,
                communication, subjects, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void studentCanRequestPackageSessionWithoutReservingIt() {
        Instant preferred = now.plusSeconds(86_400);
        when(identities.isActive(studentId)).thenReturn(true);
        when(identities.isActive(teacherUserId)).thenReturn(true);
        when(packages.inspect(packageId, studentId)).thenReturn(packageSnapshot());
        when(teachers.getTeacher(teacherId)).thenReturn(teacherSnapshot());
        when(teachers.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(requests.existsByStudentPackageIdAndStatus(packageId, BookingRequestStatus.PENDING)).thenReturn(false);
        when(requests.saveAndFlush(any(BookingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingRequestView created = service.create(studentId,
                new CreateBookingRequestCommand(packageId, preferred, "Tối thứ bảy nếu được"));

        assertThat(created.status()).isEqualTo(BookingRequestStatus.PENDING);
        assertThat(created.preferredStartTime()).isEqualTo(preferred);
        assertThat(created.studentPackageId()).isEqualTo(packageId);
        verify(packages, never()).reserveSession(packageId);
        verifyNoInteractions(bookings);
        verify(communication).publishAfterCommit(any());
    }

    @Test
    void studentCannotCreateAnotherPendingRequestForSamePackage() {
        when(identities.isActive(studentId)).thenReturn(true);
        when(identities.isActive(teacherUserId)).thenReturn(true);
        when(packages.inspect(packageId, studentId)).thenReturn(packageSnapshot());
        when(teachers.getTeacher(teacherId)).thenReturn(teacherSnapshot());
        when(teachers.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(requests.existsByStudentPackageIdAndStatus(packageId, BookingRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.create(studentId,
                new CreateBookingRequestCommand(packageId, now.plusSeconds(86_400), null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.BOOKING_REQUEST_ALREADY_PENDING));
        verify(requests, never()).saveAndFlush(any());
        verify(packages, never()).reserveSession(packageId);
    }

    @Test
    void teacherAcceptanceCreatesBookingUsingPurchasedDurationAndFinalTime() {
        Instant finalStart = now.plusSeconds(172_800);
        BookingRequest request = BookingRequest.create(packageId, studentId, teacherId, subjectId,
                now.plusSeconds(86_400), "Tối thứ bảy");
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(teacherSnapshot());
        when(requests.findByIdForUpdate(any())).thenReturn(Optional.of(request));
        when(packages.inspect(packageId, studentId)).thenReturn(packageSnapshot());
        BookingDetail createdBooking = mock(BookingDetail.class);
        when(createdBooking.id()).thenReturn(java.util.UUID.randomUUID());
        when(bookings.create(eq(teacherUserId), any(CreateBookingRequest.class))).thenReturn(createdBooking);

        service.accept(teacherUserId, java.util.UUID.randomUUID(), new AcceptBookingRequestCommand(
                finalStart, DeliveryMode.ONLINE, "https://meet.example/qa", null, 0));

        verify(bookings).create(eq(teacherUserId), org.mockito.ArgumentMatchers.argThat(command ->
                command.studentPackageId().equals(packageId)
                        && command.startTime().equals(finalStart)
                        && command.endTime().equals(finalStart.plusSeconds(45 * 60L))
                        && command.deliveryMode() == DeliveryMode.ONLINE));
        assertThat(request.getStatus()).isEqualTo(BookingRequestStatus.ACCEPTED);
        assertThat(request.getAcceptedBookingId()).isEqualTo(createdBooking.id());
    }

    @Test
    void teacherRejectsPendingRequestWithoutCreatingOrReservingBooking() {
        BookingRequest request = BookingRequest.create(packageId, studentId, teacherId, subjectId,
                now.plusSeconds(86_400), "Tối thứ bảy");
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(teacherSnapshot());
        when(requests.findByIdForUpdate(any())).thenReturn(Optional.of(request));

        var result = service.reject(teacherUserId, java.util.UUID.randomUUID(),
                new RejectBookingRequestCommand("Không còn giờ phù hợp", 0));

        assertThat(result.status()).isEqualTo(BookingRequestStatus.REJECTED);
        assertThat(result.decisionReason()).isEqualTo("Không còn giờ phù hợp");
        verifyNoInteractions(bookings);
        verify(packages, never()).reserveSession(packageId);
    }

    @Test
    void studentCanCancelOnlyTheirPendingRequest() {
        BookingRequest request = BookingRequest.create(packageId, studentId, teacherId, subjectId,
                now.plusSeconds(86_400), null);
        when(requests.findByIdForUpdate(any())).thenReturn(Optional.of(request));

        var cancelled = service.cancel(studentId, java.util.UUID.randomUUID(), 0);

        assertThat(cancelled.status()).isEqualTo(BookingRequestStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 0))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private BookingPackageSnapshot packageSnapshot() {
        return new BookingPackageSnapshot(packageId, studentId, teacherId, subjectId, "ACTIVE",
                5, 0, 2, 0, 7, 45, 700_000L, new BigDecimal("5.00"), now.plusSeconds(30L * 86_400), 0);
    }

    private TeacherSnapshot teacherSnapshot() {
        return new TeacherSnapshot(teacherId, teacherUserId, "APPROVED", true, true, "Teacher",
                "avatar.png", "Bio", 5, true, false, List.of("VIETNAMESE"), "Address", "video.mp4");
    }
}
