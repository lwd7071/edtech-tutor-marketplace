package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.dto.response.TrialRequestView;
import com.edtech.platform.booking.facade.BookingEvent;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.TrialRequestRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialRequestService {

    private final TrialRequestRepository trialRequestRepository;
    private final BookingRepository bookingRepository;
    private final TeacherFacade teacherFacade;
    private final IdentityFacade identityFacade;
    private final CommunicationFacade communicationFacade;

    @Transactional
    public TrialRequestView create(UUID studentUserId, CreateTrialRequest request) {
        Objects.requireNonNull(studentUserId, "studentUserId is required");
        Objects.requireNonNull(request, "request is required");

        if (!identityFacade.isActive(studentUserId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (request.preferredStartTime() == null || !request.preferredStartTime().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.BOOKING_IN_PAST);
        }

        TeacherSnapshot teacher = teacherFacade.getTeacher(request.teacherId());
        if (teacher == null || !"APPROVED".equalsIgnoreCase(teacher.status())) {
            throw new BusinessException(ErrorCode.TEACHER_NOT_APPROVED);
        }

        if (!teacherFacade.hasAssignedSubject(request.teacherId(), request.subjectId())) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED);
        }

        if (trialRequestRepository.existsByTeacherIdAndStudentIdAndStatus(request.teacherId(), studentUserId, TrialRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.TRIAL_REQUEST_ALREADY_PENDING, "A pending trial request already exists for this pair");
        }
        if (bookingRepository.existsByTeacherIdAndStudentIdAndStatusIn(request.teacherId(), studentUserId,
                java.util.List.of(BookingStatus.SCHEDULED, BookingStatus.COMPLETED))) {
            throw new BusinessException(ErrorCode.TRIAL_ALREADY_USED);
        }

        TrialRequest trialRequest = TrialRequest.create(
                studentUserId,
                request.teacherId(),
                request.subjectId(),
                request.preferredStartTime(),
                request.note()
        );
        try {
            trialRequest = trialRequestRepository.saveAndFlush(trialRequest);
        } catch (DataIntegrityViolationException conflict) {
            throw new BusinessException(ErrorCode.TRIAL_REQUEST_ALREADY_PENDING);
        }

        communicationFacade.publishAfterCommit(
                new BookingEvent("TRIAL_REQUESTED", trialRequest.getId(), studentUserId, teacher.id(), teacher.userId())
        );

        return TrialRequestView.from(trialRequest);
    }

    @Transactional(readOnly = true)
    public Page<TrialRequestView> find(UUID teacherUserId, Pageable pageable) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;
        if (teacherId == null) {
            return Page.empty();
        }
        return trialRequestRepository.findByTeacherId(teacherId, pageable).map(TrialRequestView::from);
    }

    @Transactional(readOnly = true)
    public Page<TrialRequestView> findForStudent(UUID studentUserId, TrialRequestStatus status, Pageable pageable) {
        Page<TrialRequest> page = status == null ? trialRequestRepository.findByStudentId(studentUserId, pageable)
                : trialRequestRepository.findByStudentIdAndStatus(studentUserId, status, pageable);
        return page.map(TrialRequestView::from);
    }

    @Transactional
    public BookingDetail accept(UUID teacherUserId, UUID requestId, AcceptTrialRequest request) {
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(requestId, "requestId is required");
        Objects.requireNonNull(request, "request is required");

        Instant start = request.startTime();
        Instant end = request.endTime();
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TIME_RANGE);
        }
        if (start.isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.BOOKING_IN_PAST);
        }

        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;

        TrialRequest trialRequest = trialRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (teacherId == null || !trialRequest.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        requireVersion(trialRequest.getVersion(), request.version());
        if (trialRequest.getStatus() != TrialRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.TRIAL_REQUEST_INVALID_STATE);
        }
        if (bookingRepository.existsByTeacherIdAndStudentIdAndStatusIn(teacherId, trialRequest.getStudentId(),
                java.util.List.of(BookingStatus.SCHEDULED, BookingStatus.COMPLETED))) {
            throw new BusinessException(ErrorCode.TRIAL_ALREADY_USED);
        }

        // Overlap checks
        if (bookingRepository.existsOverlapTeacher(teacherId, start, end)) {
            throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "Teacher has overlapping booking");
        }
        if (bookingRepository.existsOverlapStudent(trialRequest.getStudentId(), start, end)) {
            throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "Student has overlapping booking");
        }

        boolean outsideAvailability = !teacherFacade.isWithinAvailability(teacherId, start, end);

        // Create Trial Booking
        Booking booking = Booking.scheduleTrial(
                teacherId,
                trialRequest.getStudentId(),
                trialRequest.getSubjectId(),
                start,
                end,
                request.deliveryMode(),
                request.meetingLink(),
                request.locationAddress(),
                outsideAvailability
        );
        try {
            booking = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException conflict) {
            throw new BusinessException(ErrorCode.TRIAL_ALREADY_USED);
        }

        // Update trial request status
        trialRequest.accept(booking.getId(), Instant.now());

        communicationFacade.publishAfterCommit(
                new BookingEvent("TRIAL_ACCEPTED", booking.getId(), trialRequest.getStudentId(), teacherId, teacherUserId)
        );

        return BookingDetail.from(booking);
    }

    @Transactional
    public TrialRequestView reject(UUID teacherUserId, UUID requestId, com.edtech.platform.booking.dto.request.RejectTrialRequest request) {
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(requestId, "requestId is required");

        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED);
        }

        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;

        TrialRequest trialRequest = trialRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (teacherId == null || !trialRequest.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        requireVersion(trialRequest.getVersion(), request.version());
        if (trialRequest.getStatus() != TrialRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.TRIAL_REQUEST_INVALID_STATE);
        }

        trialRequest.reject(request.reason(), Instant.now());

        communicationFacade.publishAfterCommit(
                new BookingEvent("TRIAL_REJECTED", trialRequest.getId(), trialRequest.getStudentId(), teacherId, teacherUserId)
        );

        return TrialRequestView.from(trialRequest);
    }

    public TrialRequestView reject(UUID teacherUserId, UUID requestId, String reason) {
        return reject(teacherUserId, requestId, new com.edtech.platform.booking.dto.request.RejectTrialRequest(reason));
    }

    private void requireVersion(long current, Long requested) {
        if (requested == null || current != requested) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
