package com.edtech.platform.booking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
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
    public TrialRequest create(UUID studentUserId, CreateTrialRequest request) {
        Objects.requireNonNull(studentUserId, "studentUserId is required");
        Objects.requireNonNull(request, "request is required");

        if (!identityFacade.isActive(studentUserId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
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

        TrialRequest trialRequest = TrialRequest.create(
                studentUserId,
                request.teacherId(),
                request.subjectId(),
                request.preferredStartTime(),
                request.note()
        );
        trialRequest = trialRequestRepository.save(trialRequest);

        communicationFacade.publishAfterCommit(
                new BookingEvent("TRIAL_REQUESTED", trialRequest.getId(), studentUserId, request.teacherId())
        );

        return trialRequest;
    }

    @Transactional(readOnly = true)
    public Page<TrialRequest> find(UUID teacherUserId, Pageable pageable) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;
        if (teacherId == null) {
            return Page.empty();
        }
        return trialRequestRepository.findByTeacherId(teacherId, pageable);
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
        if (trialRequest.getStatus() != TrialRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.TRIAL_REQUEST_INVALID_STATE);
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
        booking = bookingRepository.save(booking);

        // Update trial request status
        trialRequest.accept(booking.getId(), Instant.now());

        communicationFacade.publishAfterCommit(
                new BookingEvent("TRIAL_ACCEPTED", booking.getId(), trialRequest.getStudentId(), teacherUserId)
        );

        return BookingDetail.from(booking);
    }

    @Transactional
    public TrialRequest reject(UUID teacherUserId, UUID requestId, String reason) {
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(requestId, "requestId is required");

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED);
        }

        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        UUID teacherId = teacher != null ? teacher.id() : null;

        TrialRequest trialRequest = trialRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (teacherId == null || !trialRequest.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (trialRequest.getStatus() != TrialRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.TRIAL_REQUEST_INVALID_STATE);
        }

        trialRequest.reject(reason, Instant.now());

        communicationFacade.publishAfterCommit(
                new BookingEvent("TRIAL_REJECTED", trialRequest.getId(), trialRequest.getStudentId(), teacherId)
        );

        return trialRequest;
    }
}