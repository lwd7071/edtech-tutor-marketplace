package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.BookingSettlementRepository;
import com.edtech.platform.booking.repository.SessionReportRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReadServiceTeacherWorkspaceTest {
    @Mock private BookingRepository bookings;
    @Mock private SessionReportRepository reports;
    @Mock private TeacherFacade teachers;
    @Mock private IdentityFacade identities;
    @Mock private SubjectFacade subjects;
    @Mock private BookingSettlementRepository settlements;
    @Mock private java.time.Clock clock;
    @InjectMocks private BookingReadService service;

    @Test
    void teacherPageUsesBatchIdentityAndSubjectSnapshotsWithoutPerBookingDetailReads() {
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        TeacherSnapshot teacher = new TeacherSnapshot(teacherProfileId, teacherUserId, "APPROVED", true, true,
                "Teacher", null, null, 3, true, false, List.of("vi"), null, null);
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(teacher);
        Booking booking = Booking.scheduleOfficial(teacherProfileId, studentId, packageId, subjectId,
                Instant.parse("2026-10-29T12:00:00Z"), Instant.parse("2026-10-29T13:00:00Z"),
                DeliveryMode.ONLINE, "https://meeting.test/room", null, false);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        when(bookings.findTeacher(teacherProfileId, false, null, false, null, false, null, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(identities.getIdentities(List.of(studentId))).thenReturn(Map.of(studentId,
                new IdentitySnapshot(studentId, "student@example.test", "Nguyễn An", "STUDENT", "ACTIVE", null, false, null)));
        when(subjects.getSubjects(List.of(subjectId))).thenReturn(Map.of(subjectId,
                new SubjectSnapshot(subjectId, "ALG", "Đại số", "HIGH_SCHOOL", true)));

        var result = service.listTeacherWorkspace(teacherUserId, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0)).containsEntry("student", Map.of("id", studentId, "fullName", "Nguyễn An"))
                .containsEntry("subject", Map.of("id", subjectId, "name", "Đại số"));
        verify(identities).getIdentities(List.of(studentId));
        verify(subjects).getSubjects(List.of(subjectId));
        verify(identities, never()).getIdentity(eq(studentId));
        verify(subjects, never()).getSubject(eq(subjectId));
        verify(reports, never()).findByBookingId(org.mockito.ArgumentMatchers.any());
        verify(settlements, never()).findByBookingIdForRead(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void upcomingTeacherWorkspaceQueriesOnlyScheduledBookingsWhoseEndTimeIsAfterNow() {
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-28T10:00:00Z");
        TeacherSnapshot teacher = new TeacherSnapshot(teacherProfileId, teacherUserId, "APPROVED", true, true,
                "Teacher", null, null, 3, true, false, List.of("vi"), null, null);
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(teacher);
        Booking booking = Booking.scheduleOfficial(teacherProfileId, studentId, packageId, subjectId,
                now.minusSeconds(900), now.plusSeconds(2_700), DeliveryMode.ONLINE, "https://meeting.test/room", null, false);
        var pageable = PageRequest.of(0, 5, Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("id")));
        when(bookings.findUpcomingTeacher(teacherProfileId, now, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(identities.getIdentities(List.of(studentId))).thenReturn(Map.of(studentId,
                new IdentitySnapshot(studentId, "student@example.test", "Nguyễn An", "STUDENT", "ACTIVE", null, false, null)));
        when(subjects.getSubjects(List.of(subjectId))).thenReturn(Map.of(subjectId,
                new SubjectSnapshot(subjectId, "ALG", "Đại số", "HIGH_SCHOOL", true)));

        var result = service.findUpcomingTeacherWorkspace(teacherUserId, now, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().get("startTime")).isEqualTo(now.minusSeconds(900));
        verify(bookings).findUpcomingTeacher(teacherProfileId, now, pageable);
        verify(identities).getIdentities(List.of(studentId));
        verify(subjects).getSubjects(List.of(subjectId));
        verify(reports, never()).findByBookingId(org.mockito.ArgumentMatchers.any());
        verify(settlements, never()).findByBookingIdForRead(org.mockito.ArgumentMatchers.any());
    }
}
