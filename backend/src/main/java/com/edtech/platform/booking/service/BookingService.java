package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.admin.facade.AuditTrailFacade;
import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.dto.request.*;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.dto.response.SessionReportView;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.facade.BookingEvent;
import com.edtech.platform.booking.facade.dto.BookingPackageSnapshot;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.SessionReportRepository;
import com.edtech.platform.booking.repository.BookingSettlementRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EnrollmentBookingFacade enrollmentBookingFacade;
    private final SessionReportRepository sessionReportRepository;
    private final FinanceFacade financeFacade;
    private final PackageMoneyAllocator packageMoneyAllocator;
    private final CommunicationFacade communicationFacade;
    private final TeacherFacade teacherFacade;
    private final IdentityFacade identityFacade;
    private final Clock clock;

    private final BookingSettlementRepository settlementRepository;
    private final AuditTrailFacade auditTrail;

    @Transactional
    public BookingDetail create(UUID teacherUserId, CreateBookingRequest request) {
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(request, "request is required");

        // 1. Validate time
        Instant start = request.startTime();
        Instant end = request.endTime();
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TIME_RANGE);
        }
        if (start.isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.BOOKING_IN_PAST);
        }

        // 2. Lock / resolve Approved Teacher
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        if (teacher == null || !"APPROVED".equalsIgnoreCase(teacher.status())) {
            throw new BusinessException(ErrorCode.TEACHER_NOT_APPROVED);
        }
        UUID teacherId = teacher.id();

        // 3. Inspect StudentPackage
        BookingPackageSnapshot pkg = enrollmentBookingFacade.inspect(request.studentPackageId(), null);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 4. Validate package invariants & relation
        if (!teacherId.equals(pkg.teacherId())) {
            throw new BusinessException(ErrorCode.PACKAGE_RELATION_MISMATCH);
        }
        if (!"ACTIVE".equalsIgnoreCase(pkg.status())) {
            throw new BusinessException(ErrorCode.PACKAGE_NOT_ACTIVE);
        }
        if (pkg.remainingSessions() <= 0) {
            throw new BusinessException(ErrorCode.PACKAGE_NO_REMAINING_SESSION);
        }
        if (end.isAfter(pkg.expiresAt())) {
            throw new BusinessException(ErrorCode.PACKAGE_BOOKING_END_AFTER_EXPIRY);
        }

        // 5. Check active Student
        if (!identityFacade.isActive(pkg.studentId())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 6. Check Teacher teaches Subject
        if (!teacherFacade.hasAssignedSubject(teacherId, pkg.subjectId())) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }

        // 7. Check Overlap
        if (bookingRepository.existsOverlapTeacher(teacherId, start, end)) {
            throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "Gia sư đã có buổi học trùng thời gian");
        }
        if (bookingRepository.existsOverlapStudent(pkg.studentId(), start, end)) {
            throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "Học viên đã có buổi học trùng thời gian");
        }

        // 8. Availability check
        boolean outsideAvailability = !teacherFacade.isWithinAvailability(teacherId, start, end);

        // 9. Reserve Session on package
        enrollmentBookingFacade.reserveSession(request.studentPackageId());

        // 10. Insert Booking
        Booking booking = Booking.scheduleOfficial(
                teacherId,
                pkg.studentId(),
                request.studentPackageId(),
                pkg.subjectId(),
                start,
                end,
                request.deliveryMode(),
                request.meetingLink(),
                request.locationAddress(),
                outsideAvailability
        );
        booking = bookingRepository.save(booking);

        // Create the settlement snapshot at booking time so expiry processing never
        // has to reconstruct money from a mutable package or current catalog data.
        if (!booking.isTrial()) {
            settlementRepository.save(BookingSettlement.awaiting(
                    booking.getId(),
                    booking.getEndTime().plusSeconds(86400),
                    null
            ));
        }

        // 11. Publish Event after commit
        communicationFacade.publishAfterCommit(
                new BookingEvent("BOOKING_CREATED", booking.getId(), booking.getStudentId(), teacherId, teacherUserId)
        );

        return BookingDetail.from(booking);
    }

    @Transactional
    public BookingDetail complete(UUID teacherUserId, UUID bookingId, CompleteBookingRequest request) {
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(bookingId, "bookingId is required");
        Objects.requireNonNull(request, "request is required");

        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (teacherId == null || !booking.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        if (booking.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        BookingSettlement existingSettlement = !booking.isTrial()
                ? settlementRepository.findByBookingId(bookingId).orElse(null) : null;
        boolean reopened = existingSettlement != null && existingSettlement.getStatus() == SettlementStatus.REOPENED;
        if (booking.getStatus() != BookingStatus.SCHEDULED && !reopened) throw new BusinessException(ErrorCode.BOOKING_INVALID_STATE);
        if (booking.getEndTime().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_ENDED);
        }
        if (!booking.isTrial() && !clock.instant().isAfter(booking.getEndTime()))
            throw new BusinessException(ErrorCode.BOOKING_CONFIRMATION_TOO_EARLY);
        if (request.report() == null) {
            throw new BusinessException(ErrorCode.BOOKING_REPORT_REQUIRED);
        }
        if (!booking.isTrial()) {
            if (existingSettlement == null) existingSettlement = settlementRepository.save(BookingSettlement.awaiting(
                    bookingId, booking.getEndTime().plusSeconds(86400), null));
            existingSettlement.requireConfirmationWindow(clock.instant());
            if (existingSettlement.getTeacherConfirmedAt() != null)
                throw new BusinessException(ErrorCode.BOOKING_CONFIRMATION_ALREADY_EXISTS);
        }
        if (sessionReportRepository.existsByBookingId(bookingId)) {
            throw new BusinessException(ErrorCode.SESSION_REPORT_ALREADY_EXISTS);
        }

        // 1. Create SessionReport
        SessionReport report = SessionReport.create(
                bookingId,
                request.report().recordLink(),
                request.report().content(),
                request.report().feedback(),
                request.report().followUpNote(),
                request.report().teacherSelfRating(),
                clock.instant()
        );
        sessionReportRepository.save(report);

        // Trial keeps its original one-party completion semantics.
        if (booking.isTrial()) {
            booking.complete(clock.instant());
        }
        // 3. Settle Package & Finance if official booking
        if (!booking.isTrial()) {
            existingSettlement.confirmTeacher(clock.instant());
            if (existingSettlement.bothConfirmed()) {
                BookingPackageSnapshot pkg = enrollmentBookingFacade.inspect(booking.getStudentPackageId(), booking.getStudentId());
                completeAndRelease(booking, existingSettlement, pkg);
            }
        }

        // 4. Publish Event after commit
        communicationFacade.publishAfterCommit(
                new BookingEvent(booking.getStatus() == BookingStatus.COMPLETED ? "BOOKING_COMPLETED" : "BOOKING_TEACHER_CONFIRMED",
                        booking.getId(), booking.getStudentId(), teacherId, teacherUserId)
        );

        return BookingDetail.from(booking);
    }

    private long sessionAmount(BookingPackageSnapshot pkg) {
        long total = packageMoneyAllocator.teacherNetTotal(pkg.purchasePriceVnd(), pkg.commissionRate() != null ? pkg.commissionRate() : java.math.BigDecimal.ZERO);
        return packageMoneyAllocator.allocationForRange(total, pkg.totalSessions(), pkg.completedSessions() + pkg.refundedSessions(), 1);
    }

    private void completeAndRelease(Booking booking, BookingSettlement settlement, BookingPackageSnapshot pkg) {
        SettlementStatus before = settlement.getStatus();
        if (booking.getStatus() == BookingStatus.SCHEDULED) {
            settlement.allocateAmount(sessionAmount(pkg));
            enrollmentBookingFacade.completeReservedSession(booking.getStudentPackageId());
            booking.complete(clock.instant());
            if (!booking.isSettlementProcessed()) booking.markSettlementProcessed();
        }
        long amount = Objects.requireNonNull(settlement.getNetAmountVnd(), "consumed settlement amount is required");
        if (amount > 0) {
            if (before == SettlementStatus.REOPENED) financeFacade.releaseHeldBookingSession(booking.getTeacherId(), booking.getId(), amount);
            else financeFacade.settleBookingSession(booking.getTeacherId(), booking.getId(), amount);
        }
        settlement.release();
    }

    @Transactional
    public BookingDetail confirmByStudent(UUID studentId, UUID bookingId, long version) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getStudentId().equals(studentId)) throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        if (booking.isTrial()) throw new BusinessException(ErrorCode.BOOKING_INVALID_STATE);
        if (!clock.instant().isAfter(booking.getEndTime())) throw new BusinessException(ErrorCode.BOOKING_CONFIRMATION_TOO_EARLY);
        if (booking.getVersion() != version) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        BookingSettlement settlement = settlementRepository.findByBookingId(bookingId).orElseGet(() -> settlementRepository.save(BookingSettlement.awaiting(bookingId, booking.getEndTime().plusSeconds(86400), null)));
        Instant now = clock.instant();
        settlement.confirmStudent(now);
        if (settlement.bothConfirmed()) {
            BookingPackageSnapshot pkg = enrollmentBookingFacade.inspect(booking.getStudentPackageId(), studentId);
            completeAndRelease(booking, settlement, pkg);
        }
        return BookingDetail.from(booking);
    }

    @Transactional
    public BookingDetail dispute(UUID teacherUserId, UUID bookingId, long version, String reason) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        if (teacher == null || !booking.getTeacherId().equals(teacher.id())) throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        if (booking.getVersion() != version) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        BookingSettlement s = settlementRepository.findByBookingId(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE));
        s.dispute(reason, clock.instant());
        return BookingDetail.from(booking);
    }

    @Transactional
    public BookingDetail reopenSettlement(UUID actorId, UUID bookingId, long version, String note) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        BookingSettlement s = settlementRepository.findByBookingId(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE));
        if (s.getVersion() != version) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        if (note == null || note.isBlank()) throw new BusinessException(ErrorCode.BOOKING_DISPUTE_REASON_REQUIRED);
        SettlementStatus before = s.getStatus();
        s.reopen(clock.instant());
        auditTrail.append(actorId, "BOOKING_SETTLEMENT_REOPENED", "BOOKING_SETTLEMENT", s.getId(),
                java.util.Map.of("status", before.name()),
                java.util.Map.of("status", s.getStatus().name(), "bookingId", bookingId,
                        "netAmountVnd", Objects.requireNonNullElse(s.getNetAmountVnd(), 0L), "note", note));
        return BookingDetail.from(booking);
    }

    @Transactional
    public BookingDetail adminDecision(UUID actorId, UUID bookingId, long version, boolean retain, String note) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        BookingSettlement s = settlementRepository.findByBookingId(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE));
        if (s.getVersion() != version) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        if (note == null || note.isBlank()) throw new BusinessException(ErrorCode.BOOKING_DISPUTE_REASON_REQUIRED);
        if (s.getStatus() == SettlementStatus.REOPENED && s.getReopenDeadline() != null
                && !clock.instant().isBefore(s.getReopenDeadline()) && !s.bothConfirmed())
            s.awaitingAdminDecision();
        if (!retain && s.getStatus() != SettlementStatus.AWAITING_ADMIN_DECISION) throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE);
        if (retain && s.getStatus() != SettlementStatus.AWAITING_ADMIN_DECISION && s.getStatus() != SettlementStatus.DISPUTE_PENDING) throw new BusinessException(ErrorCode.BOOKING_SETTLEMENT_INVALID_STATE);
        SettlementStatus before = s.getStatus();
        long amount = s.getNetAmountVnd() == null ? 0 : s.getNetAmountVnd();
        if (retain) {
            financeFacade.retainHeldBookingSession(booking.getId(), amount);
        } else {
            financeFacade.releaseHeldBookingSession(booking.getTeacherId(), booking.getId(), amount);
        }
        s.adminDecision(retain);
        auditTrail.append(actorId, retain ? "BOOKING_SETTLEMENT_RETAINED" : "BOOKING_SETTLEMENT_RELEASED",
                "BOOKING_SETTLEMENT", s.getId(), java.util.Map.of("status", before.name()),
                java.util.Map.of("status", s.getStatus().name(), "bookingId", bookingId,
                        "netAmountVnd", amount, "note", note));
        return BookingDetail.from(booking);
    }

    @Transactional
    public BookingDetail cancel(UUID teacherUserId, UUID bookingId, CancelBookingRequest request) {
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(bookingId, "bookingId is required");
        Objects.requireNonNull(request, "request is required");

        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (teacherId == null || !booking.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        if (booking.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (booking.getStatus() != BookingStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATE);
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED);
        }

        // 1. Cancel Booking
        booking.cancel(request.reason(), request.initiatedBy(), clock.instant());

        // 2. Release reserved session if official
        if (!booking.isTrial()) {
            enrollmentBookingFacade.releaseReservedSession(booking.getStudentPackageId());
        }

        // 3. Publish Event after commit
        communicationFacade.publishAfterCommit(
                new BookingEvent("BOOKING_CANCELLED", booking.getId(), booking.getStudentId(), teacherId, teacherUserId)
        );

        return BookingDetail.from(booking);
    }

}
