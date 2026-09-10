package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
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
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            throw new BusinessException(ErrorCode.PACKAGE_INVALID_STATE);
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
            throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "Teacher has overlapping booking");
        }
        if (bookingRepository.existsOverlapStudent(pkg.studentId(), start, end)) {
            throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "Student has overlapping booking");
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

        // 11. Publish Event after commit
        communicationFacade.publishAfterCommit(
                new BookingEvent("BOOKING_CREATED", booking.getId(), booking.getStudentId(), teacherUserId)
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
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATE);
        }
        if (booking.getStatus() != BookingStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATE);
        }
        if (booking.getEndTime().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_ENDED);
        }
        if (request.report() == null) {
            throw new BusinessException(ErrorCode.BOOKING_REPORT_REQUIRED);
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

        // 2. Complete Booking
        booking.complete(clock.instant());

        // 3. Settle Package & Finance if official booking
        if (!booking.isTrial()) {
            BookingPackageSnapshot pkg = enrollmentBookingFacade.inspect(booking.getStudentPackageId(), booking.getStudentId());
            enrollmentBookingFacade.completeReservedSession(booking.getStudentPackageId());

            long teacherNetTotal = packageMoneyAllocator.teacherNetTotal(
                    pkg.purchasePriceVnd(),
                    pkg.commissionRate() != null ? pkg.commissionRate() : java.math.BigDecimal.ZERO
            );
            int resolvedBefore = pkg.completedSessions() + pkg.refundedSessions();
            long sessionNet = packageMoneyAllocator.allocationForRange(
                    teacherNetTotal, pkg.totalSessions(), resolvedBefore, 1);
            if (sessionNet > 0) {
                financeFacade.settleBookingSession(booking.getTeacherId(), bookingId, sessionNet);
            }
            booking.markSettlementProcessed();
        }

        // 4. Publish Event after commit
        communicationFacade.publishAfterCommit(
                new BookingEvent("BOOKING_COMPLETED", booking.getId(), booking.getStudentId(), teacherUserId)
        );

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
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATE);
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
                new BookingEvent("BOOKING_CANCELLED", booking.getId(), booking.getStudentId(), teacherUserId)
        );

        return BookingDetail.from(booking);
    }

}
