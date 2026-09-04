package com.edtech.platform.booking.service;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingJobsTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CommunicationFacade communicationFacade;
    @Mock private PlatformSettingsFacade platformSettingsFacade;
    @Mock private EnrollmentBookingFacade enrollmentBookingFacade;
    @Mock private TransactionTemplate transactionTemplate;

    @Test
    void reminderJob_shouldPublishReminders() {
        BookingReminderJob job = new BookingReminderJob(bookingRepository, communicationFacade, platformSettingsFacade);

        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Booking booking = Booking.scheduleTrial(teacherId, studentId, UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600), DeliveryMode.ONLINE, null, null, false);

        when(bookingRepository.findReminderCandidates(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(booking)));

        job.publish();

        verify(communicationFacade).sendBookingReminder(eq(teacherId), any());
        verify(communicationFacade).sendBookingReminder(eq(studentId), any());
    }

    @Test
    void expiryJob_shouldExpireOverdueBookings_andReleasePackageSession() {
        BookingExpiryJob job = new BookingExpiryJob(bookingRepository, enrollmentBookingFacade, transactionTemplate);

        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        Booking booking = spy(Booking.scheduleOfficial(teacherId, studentId, packageId, UUID.randomUUID(), Instant.now().minusSeconds(86400), Instant.now().minusSeconds(80000), DeliveryMode.ONLINE, null, null, false));

        when(bookingRepository.findExpiryCandidates(any(), any()))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.findByIdForUpdate(any())).thenReturn(Optional.of(booking));
        doAnswer(inv -> {
            Consumer<TransactionStatus> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        job.expire();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(enrollmentBookingFacade).releaseReservedSession(packageId);
    }

    @Test
    void packageExpiryJob_shouldInvokeLockExpiredPackages() {
        PackageExpiryJob job = new PackageExpiryJob(enrollmentBookingFacade);

        job.lockExpired();

        verify(enrollmentBookingFacade).lockExpiredPackages(any(), any());
    }
}