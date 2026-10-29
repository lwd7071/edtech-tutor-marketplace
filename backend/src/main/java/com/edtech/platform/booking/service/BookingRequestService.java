package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.domain.BookingRequest;
import com.edtech.platform.booking.domain.BookingRequestStatus;
import com.edtech.platform.booking.dto.request.AcceptBookingRequestCommand;
import com.edtech.platform.booking.dto.request.CreateBookingRequest;
import com.edtech.platform.booking.dto.request.CreateBookingRequestCommand;
import com.edtech.platform.booking.dto.request.RejectBookingRequestCommand;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.dto.response.BookingRequestView;
import com.edtech.platform.booking.facade.BookingEvent;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.facade.dto.BookingPackageSnapshot;
import com.edtech.platform.booking.repository.BookingRequestRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.booking.web.TeacherBookingRequestRow;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingRequestService {
    private final BookingRequestRepository requests;
    private final EnrollmentBookingFacade packages;
    private final TeacherFacade teachers;
    private final IdentityFacade identities;
    private final BookingService bookings;
    private final CommunicationFacade communication;
    private final SubjectFacade subjects;
    private final Clock clock;

    @Transactional
    public BookingRequestView create(UUID studentId, CreateBookingRequestCommand command) {
        if (studentId == null || command == null) throw new IllegalArgumentException("student and request are required");
        Instant now = clock.instant();
        if (command.preferredStartTime() == null || !command.preferredStartTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.BOOKING_IN_PAST);
        }
        if (command.studentNote() != null && command.studentNote().length() > 500) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (!identities.isActive(studentId)) throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);

        BookingPackageSnapshot pkg = packages.inspect(command.studentPackageId(), studentId);
        if (pkg == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        requireBookablePackage(pkg, command.preferredStartTime());

        TeacherSnapshot teacher = teachers.getTeacher(pkg.teacherId());
        if (teacher == null || !"APPROVED".equalsIgnoreCase(teacher.status())) {
            throw new BusinessException(ErrorCode.TEACHER_NOT_APPROVED);
        }
        if (!identities.isActive(teacher.userId())) throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        if (!teachers.hasAssignedSubject(pkg.teacherId(), pkg.subjectId())) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }
        if (requests.existsByStudentPackageIdAndStatus(pkg.packageId(), BookingRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_ALREADY_PENDING);
        }

        BookingRequest request = BookingRequest.create(pkg.packageId(), studentId, pkg.teacherId(), pkg.subjectId(),
                command.preferredStartTime(), blankToNull(command.studentNote()));
        try {
            request = requests.saveAndFlush(request);
        } catch (DataIntegrityViolationException conflict) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_ALREADY_PENDING);
        }
        communication.publishAfterCommit(new BookingEvent(
                "BOOKING_REQUESTED", request.getId(), studentId, pkg.teacherId(), teacher.userId()));
        return BookingRequestView.from(request);
    }

    @Transactional(readOnly = true)
    public Page<BookingRequestView> findForStudent(UUID studentId, BookingRequestStatus status, Pageable pageable) {
        Page<BookingRequest> page = status == null ? requests.findByStudentId(studentId, pageable)
                : requests.findByStudentIdAndStatus(studentId, status, pageable);
        return page.map(BookingRequestView::from);
    }

    @Transactional(readOnly = true)
    public BookingRequestView findPendingForStudentPackage(UUID studentId, UUID packageId) {
        return requests.findFirstByStudentPackageIdAndStudentIdAndStatusOrderByCreatedAtDesc(
                        packageId, studentId, BookingRequestStatus.PENDING)
                .map(BookingRequestView::from).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<BookingRequestView> findForTeacher(UUID teacherUserId, BookingRequestStatus status, Pageable pageable) {
        TeacherSnapshot teacher = teachers.getTeacherByUserId(teacherUserId);
        if (teacher == null) return Page.empty(pageable);
        Page<BookingRequest> page = status == null ? requests.findByTeacherId(teacher.id(), pageable)
                : requests.findByTeacherIdAndStatus(teacher.id(), status, pageable);
        return page.map(BookingRequestView::from);
    }

    @Transactional(readOnly = true)
    public Page<TeacherBookingRequestRow> findTeacherWorkspace(UUID teacherUserId, BookingRequestStatus status,
                                                               Pageable pageable) {
        TeacherSnapshot teacher = teachers.getTeacherByUserId(teacherUserId);
        if (teacher == null) return Page.empty(pageable);
        Page<BookingRequest> page = status == null ? requests.findByTeacherId(teacher.id(), pageable)
                : requests.findByTeacherIdAndStatus(teacher.id(), status, pageable);
        if (page.isEmpty()) return Page.empty(pageable);

        Map<UUID, com.edtech.platform.auth.facade.dto.IdentitySnapshot> students = identities.getIdentities(
                page.getContent().stream().map(BookingRequest::getStudentId).distinct().toList());
        Map<UUID, com.edtech.platform.subject.facade.dto.SubjectSnapshot> subjectById = subjects.getSubjects(
                page.getContent().stream().map(BookingRequest::getSubjectId).distinct().toList());
        return page.map(request -> {
            var student = students.get(request.getStudentId());
            var subject = subjectById.get(request.getSubjectId());
            return new TeacherBookingRequestRow(BookingRequestView.from(request),
                    student == null || student.fullName() == null || student.fullName().isBlank()
                            ? "Học viên" : student.fullName(),
                    subject == null || subject.name() == null || subject.name().isBlank()
                            ? "Môn học" : subject.name());
        });
    }

    @Transactional
    public BookingRequestView cancel(UUID studentId, UUID requestId, long expectedVersion) {
        BookingRequest request = lock(requestId);
        if (!request.getStudentId().equals(studentId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        requireVersion(request.getVersion(), expectedVersion);
        request.cancel();
        return BookingRequestView.from(request);
    }

    @Transactional
    public BookingDetail accept(UUID teacherUserId, UUID requestId, AcceptBookingRequestCommand command) {
        if (command == null) throw new IllegalArgumentException("accept command is required");
        if (command.deliveryMode() == null || tooLong(command.meetingLink(), 500)
                || tooLong(command.locationAddress(), 500)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Instant start = command.startTime();
        if (start == null || !start.isAfter(clock.instant())) throw new BusinessException(ErrorCode.BOOKING_IN_PAST);

        TeacherSnapshot teacher = teachers.getTeacherByUserId(teacherUserId);
        BookingRequest request = lock(requestId);
        requireTeacherOwns(teacher, request);
        requireVersion(request.getVersion(), command.version());
        BookingPackageSnapshot pkg = packages.inspect(request.getStudentPackageId(), request.getStudentId());
        if (pkg == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        requireBookablePackage(pkg, start);
        Instant end = start.plusSeconds(pkg.sessionDurationMinutesSnapshot() * 60L);

        BookingDetail booking = bookings.create(teacherUserId, new CreateBookingRequest(
                pkg.packageId(), start, end, command.deliveryMode(),
                blankToNull(command.meetingLink()), blankToNull(command.locationAddress())));
        request.accept(booking.id());
        return booking;
    }

    @Transactional
    public BookingRequestView reject(UUID teacherUserId, UUID requestId, RejectBookingRequestCommand command) {
        if (command == null) throw new IllegalArgumentException("reject command is required");
        if (command.reason() == null || command.reason().isBlank()) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_REASON_REQUIRED);
        }
        if (tooLong(command.reason(), 1000)) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        TeacherSnapshot teacher = teachers.getTeacherByUserId(teacherUserId);
        BookingRequest request = lock(requestId);
        requireTeacherOwns(teacher, request);
        requireVersion(request.getVersion(), command.version());
        request.reject(command.reason());
        communication.publishAfterCommit(new BookingEvent(
                "BOOKING_REQUEST_REJECTED", request.getId(), request.getStudentId(), request.getTeacherId(), teacherUserId));
        return BookingRequestView.from(request);
    }

    private BookingRequest lock(UUID id) {
        return requests.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_REQUEST_NOT_FOUND));
    }

    private void requireTeacherOwns(TeacherSnapshot teacher, BookingRequest request) {
        if (teacher == null || !teacher.id().equals(request.getTeacherId())) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_NOT_FOUND);
        }
    }

    private void requireBookablePackage(BookingPackageSnapshot pkg, Instant start) {
        if (!"ACTIVE".equalsIgnoreCase(pkg.status())) throw new BusinessException(ErrorCode.PACKAGE_NOT_ACTIVE);
        if (pkg.remainingSessions() <= 0) throw new BusinessException(ErrorCode.PACKAGE_NO_REMAINING_SESSION);
        if (pkg.expiresAt() == null || !pkg.expiresAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.PACKAGE_EXPIRED);
        }
        if (pkg.sessionDurationMinutesSnapshot() <= 0) throw new BusinessException(ErrorCode.BOOKING_INVALID_TIME_RANGE);
        Instant end = start.plusSeconds(pkg.sessionDurationMinutesSnapshot() * 60L);
        if (end.isAfter(pkg.expiresAt())) throw new BusinessException(ErrorCode.PACKAGE_BOOKING_END_AFTER_EXPIRY);
    }

    private void requireVersion(long actual, long expected) {
        if (actual != expected) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean tooLong(String value, int limit) {
        return value != null && value.length() > limit;
    }
}
