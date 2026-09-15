package com.edtech.platform.booking.service;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.booking.domain.Booking;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.BookingSettlementRepository;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.Clock;
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
    @Mock private BookingSettlementRepository settlementRepository;
    @Mock private FinanceFacade financeFacade;
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
    void expiryJob_shouldExpireTrialAfterTwelveHours() {
        BookingExpiryJob job = new BookingExpiryJob(bookingRepository, settlementRepository, enrollmentBookingFacade,
                financeFacade, new PackageMoneyAllocator(), transactionTemplate, Clock.systemUTC());

        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Booking booking = spy(Booking.scheduleTrial(teacherId, studentId, UUID.randomUUID(),
                Instant.now().minusSeconds(50000), Instant.now().minusSeconds(49000),
                DeliveryMode.ONLINE, null, null, false));

        when(bookingRepository.findTrialExpiryCandidates(any(), any()))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.findPaidExpiryCandidates(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(settlementRepository.findReopenedExpired(any(), any())).thenReturn(List.of());
        when(bookingRepository.findByIdForUpdate(any())).thenReturn(Optional.of(booking));
        doAnswer(inv -> {
            Consumer<TransactionStatus> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        job.expire();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verifyNoInteractions(enrollmentBookingFacade, financeFacade);
    }

    @Test
    void packageExpiryJob_shouldInvokeLockExpiredPackages() {
        PackageExpiryJob job = new PackageExpiryJob(enrollmentBookingFacade);

        job.lockExpired();

        verify(enrollmentBookingFacade).lockExpiredPackages(any(), any());
    }
}
