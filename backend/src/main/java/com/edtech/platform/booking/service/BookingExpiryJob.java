package com.edtech.platform.booking.service;

import com.edtech.platform.booking.domain.BookingSettlement;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.SettlementStatus;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.BookingSettlementRepository;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingExpiryJob {
    private final BookingRepository bookings;
    private final BookingSettlementRepository settlements;
    private final EnrollmentBookingFacade packages;
    private final FinanceFacade finance;
    private final PackageMoneyAllocator allocator;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Scheduled(fixedDelay = 300000)
    @SchedulerLock(name = "BookingExpiryJob_expire", lockAtMostFor = "5m", lockAtLeastFor = "10s")
    public void expire() {
        Instant now = Instant.now(clock);
        try {
            bookings.findTrialExpiryCandidates(now.minus(Duration.ofHours(12)), PageRequest.of(0, 100))
                    .forEach(candidate -> runOne(candidate.getId(), () -> expireTrial(candidate.getId())));
            bookings.findPaidExpiryCandidates(now.minus(Duration.ofHours(24)), PageRequest.of(0, 100))
                    .forEach(candidate -> runOne(candidate.getId(), () -> holdPaid(candidate.getId())));
            settlements.findReopenedExpired(now, PageRequest.of(0, 100))
                    .forEach(candidate -> runOne(candidate.getBookingId(), () -> expireReopened(candidate.getId())));
        } catch (Exception e) {
            log.error("Booking expiry candidate scan failed", e);
        }
    }

    private void runOne(UUID bookingId, Runnable action) {
        try {
            tx.executeWithoutResult(status -> action.run());
        } catch (Exception e) {
            log.error("Booking expiry failed for booking {}", bookingId, e);
        }
    }

    private void expireTrial(UUID bookingId) {
        bookings.findByIdForUpdate(bookingId).ifPresent(booking -> {
            Instant now = Instant.now(clock);
            if (booking.isTrial() && booking.getStatus() == BookingStatus.SCHEDULED
                    && !now.isBefore(booking.getEndTime().plus(Duration.ofHours(12)))) booking.expire(now);
        });
    }

    private void holdPaid(UUID bookingId) {
        bookings.findByIdForUpdate(bookingId).ifPresent(booking -> {
            if (booking.isTrial() || booking.getStatus() != BookingStatus.SCHEDULED) return;
            BookingSettlement settlement = settlements.findByBookingId(bookingId)
                    .orElseGet(() -> settlements.save(BookingSettlement.awaiting(
                            bookingId, booking.getEndTime().plus(Duration.ofHours(24)), null)));
            Instant now = Instant.now(clock);
            if (settlement.getStatus() != SettlementStatus.AWAITING_CONFIRMATION
                    || now.isBefore(settlement.getInitialDeadline())) return;

            var snapshot = packages.inspect(booking.getStudentPackageId(), booking.getStudentId());
            long totalNet = allocator.teacherNetTotal(snapshot.purchasePriceVnd(),
                    snapshot.commissionRate() == null ? BigDecimal.ZERO : snapshot.commissionRate());
            long amount = allocator.allocationForRange(totalNet, snapshot.totalSessions(),
                    snapshot.completedSessions() + snapshot.refundedSessions(), 1);
            settlement.allocateAmount(amount);
            packages.completeReservedSession(booking.getStudentPackageId());
            booking.complete(now);
            if (!booking.isSettlementProcessed()) booking.markSettlementProcessed();
            if (amount > 0) finance.holdBookingSession(booking.getTeacherId(), bookingId, amount);
            settlement.hold();
        });
    }

    private void expireReopened(UUID settlementId) {
        BookingSettlement settlement = settlements.findByIdForUpdate(settlementId).orElse(null);
        if (settlement == null) return;
        Instant now = Instant.now(clock);
        if (settlement.getStatus() == SettlementStatus.REOPENED && !settlement.bothConfirmed()
                && settlement.getReopenDeadline() != null && !now.isBefore(settlement.getReopenDeadline()))
            settlement.awaitingAdminDecision();
    }
}
