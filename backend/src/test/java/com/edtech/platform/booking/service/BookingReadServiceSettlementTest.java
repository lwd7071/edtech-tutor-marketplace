package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.dto.response.BookingSettlementAdminView;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.BookingSettlementRepository;
import com.edtech.platform.booking.repository.SessionReportRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.subject.facade.SubjectFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingReadServiceSettlementTest {
    @Mock BookingRepository bookings;
    @Mock SessionReportRepository reports;
    @Mock TeacherFacade teachers;
    @Mock IdentityFacade identities;
    @Mock SubjectFacade subjects;
    @Mock BookingSettlementRepository settlements;
    BookingReadService service;

    @BeforeEach void setUp() {
        service = new BookingReadService(bookings, reports, teachers, identities, subjects, settlements,
                Clock.fixed(Instant.parse("2026-09-23T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test void mapsPageUsingBatchLookupsAndPreservesPageOrder() {
        UUID booking1 = UUID.randomUUID(), booking2 = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID(), teacher2 = UUID.randomUUID();
        UUID student1 = UUID.randomUUID(), student2 = UUID.randomUUID();
        var s1 = BookingSettlement.awaiting(booking1, Instant.parse("2026-09-24T00:00:00Z"), 10L);
        var s2 = BookingSettlement.awaiting(booking2, Instant.parse("2026-09-24T00:00:00Z"), 20L);
        s2.hold();
        Instant disputedAt = Instant.parse("2026-09-22T14:00:00Z");
        s2.dispute("Học viên chưa xác nhận", disputedAt);
        when(settlements.findByStatus(null, PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(s2, s1)));
        Booking firstBooking = booking(booking1, teacher1, student1);
        Booking secondBooking = booking(booking2, teacher2, student2);
        when(bookings.findAllById(List.of(booking2, booking1))).thenReturn(List.of(firstBooking, secondBooking));
        when(teachers.getTeachers(List.of(teacher2, teacher1))).thenReturn(Map.of(
                teacher1, teacher("Teacher One"), teacher2, teacher("Teacher Two")));
        when(identities.getIdentities(List.of(student2, student1))).thenReturn(Map.of(
                student1, identity("Student One"), student2, identity("Student Two")));

        var result = service.adminSettlements(null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(BookingSettlementAdminView::bookingId)
                .containsExactly(booking2, booking1);
        assertThat(result.getContent()).extracting(BookingSettlementAdminView::studentName)
                .containsExactly("Student Two", "Student One");
        assertThat(result.getContent().get(0).bookingStatus()).isEqualTo(BookingStatus.SCHEDULED);
        assertThat(result.getContent().get(0).netAmountVnd()).isEqualTo(20L);
        assertThat(result.getContent().get(0).status()).isEqualTo(SettlementStatus.DISPUTE_PENDING);
        assertThat(result.getContent().get(0).disputeReason()).isEqualTo("Học viên chưa xác nhận");
        assertThat(result.getContent().get(0).disputedAt()).isEqualTo(disputedAt);
        assertThat(result.getContent().get(0).version()).isZero();
        verify(bookings, times(1)).findAllById(List.of(booking2, booking1));
        verify(teachers, times(1)).getTeachers(List.of(teacher2, teacher1));
        verify(identities, times(1)).getIdentities(List.of(student2, student1));
    }

    @Test void emptyPageDoesNotCallBookingOrIdentityFacades() {
        when(settlements.findByStatus(null, PageRequest.of(0, 20))).thenReturn(Page.empty(PageRequest.of(0, 20)));
        assertThat(service.adminSettlements(null, PageRequest.of(0, 20))).isEmpty();
        verifyNoInteractions(bookings, teachers, identities);
    }

    @Test void missingBookingFailsWithBookingNotFound() {
        UUID bookingId = UUID.randomUUID();
        when(settlements.findByStatus(null, PageRequest.of(0, 20))).thenReturn(
                new PageImpl<>(List.of(BookingSettlement.awaiting(bookingId, Instant.now(), 1L))));
        when(bookings.findAllById(List.of(bookingId))).thenReturn(List.of());
        assertThatThrownBy(() -> service.adminSettlements(null, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(com.edtech.platform.common.exception.ErrorCode.BOOKING_NOT_FOUND));
    }

    @Test void missingOrBlankProfilesUseSafeRoleNames() {
        UUID bookingId = UUID.randomUUID(), teacherId = UUID.randomUUID(), studentId = UUID.randomUUID();
        var settlement = BookingSettlement.awaiting(bookingId, Instant.parse("2026-09-24T00:00:00Z"), null);
        Booking booking = booking(bookingId, teacherId, studentId);
        when(settlements.findByStatus(null, PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(settlement)));
        when(bookings.findAllById(List.of(bookingId))).thenReturn(List.of(booking));
        when(teachers.getTeachers(List.of(teacherId))).thenReturn(Map.of(teacherId, teacher(" ")));
        when(identities.getIdentities(List.of(studentId))).thenReturn(Map.of(studentId, identity("")));

        var view = service.adminSettlements(null, PageRequest.of(0, 20)).getContent().getFirst();
        assertThat(view.teacherName()).isEqualTo("Gia sư");
        assertThat(view.studentName()).isEqualTo("Học viên");
        assertThat(view.netAmountVnd()).isNull();
    }

    private static Booking booking(UUID id, UUID teacherId, UUID studentId) {
        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(id);
        when(booking.getTeacherId()).thenReturn(teacherId);
        when(booking.getStudentId()).thenReturn(studentId);
        when(booking.getStatus()).thenReturn(BookingStatus.SCHEDULED);
        when(booking.getStartTime()).thenReturn(Instant.parse("2026-09-22T12:00:00Z"));
        when(booking.getEndTime()).thenReturn(Instant.parse("2026-09-22T13:00:00Z"));
        return booking;
    }

    private static TeacherSnapshot teacher(String name) {
        return new TeacherSnapshot(UUID.randomUUID(), UUID.randomUUID(), "APPROVED", true, true, name,
                null, null, 0, true, false, List.of(), null, null);
    }

    private static IdentitySnapshot identity(String name) {
        return new IdentitySnapshot(UUID.randomUUID(), "user@example.test", name, "STUDENT", "ACTIVE", null,
                false, null);
    }
}
