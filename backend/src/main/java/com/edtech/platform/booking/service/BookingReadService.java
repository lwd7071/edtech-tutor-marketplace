package com.edtech.platform.booking.service;

import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.SessionReportRepository;
import com.edtech.platform.booking.repository.BookingSettlementRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.edtech.platform.booking.dto.response.SessionReportView;

@Service
@RequiredArgsConstructor
public class BookingReadService {
    private final BookingRepository bookings;
    private final SessionReportRepository reports;
    private final TeacherFacade teachers;
    private final IdentityFacade identities;
    private final SubjectFacade subjects;
    private final BookingSettlementRepository settlements;
    private final Clock clock;

    @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(UUID userId, boolean teacherRole, BookingStatus status, Instant from, Instant to, Pageable pageable) {
        return (teacherRole ? bookings.findTeacher(teacherId(userId),status,from,to,pageable)
                : bookings.findStudent(userId,status,from,to,pageable)).map(b -> view(b, teacherRole));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> detail(UUID userId, UUID id, boolean teacherRole) {
        Booking b=bookings.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (teacherRole ? !b.getTeacherId().equals(teacherId(userId)) : !b.getStudentId().equals(userId))
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return view(b, teacherRole);
    }

    @Transactional(readOnly = true)
    public Page<SessionReportView> findStudentSessionReports(UUID studentId, Pageable pageable) {
        Page<SessionReport> page = reports.findByStudentId(studentId, pageable);
        if (page.isEmpty()) return Page.empty(pageable);
        Map<UUID, Booking> bookingById = bookings.findAllById(
                        page.map(SessionReport::getBookingId).getContent()).stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));
        return page.map(report -> SessionReportView.from(report, bookingById.get(report.getBookingId())));
    }

    private UUID teacherId(UUID userId) {
        var t=teachers.getTeacherByUserId(userId);
        if(t==null)throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        return t.id();
    }

    public Page<Map<String,Object>> adminSettlements(SettlementStatus status, Pageable pageable) {
        return settlements.findByStatus(status, pageable).map(s -> bookings.findById(s.getBookingId())
                .map(b -> adminView(b, s)).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Map<String,Object> adminDetail(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        return adminView(booking, settlements.findByBookingIdForRead(bookingId).orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND)));
    }

    private Map<String,Object> adminView(Booking b, BookingSettlement s) {
        var result = new LinkedHashMap<String,Object>();
        result.put("bookingId", b.getId());
        result.put("studentId", b.getStudentId());
        result.put("teacherId", b.getTeacherId());
        result.put("status", s.getStatus());
        result.put("teacherConfirmedAt", s.getTeacherConfirmedAt());
        result.put("studentConfirmedAt", s.getStudentConfirmedAt());
        result.put("confirmationDeadline", s.getInitialDeadline());
        result.put("reopenDeadline", s.getReopenDeadline());
        result.put("netAmountVnd", s.getNetAmountVnd());
        result.put("disputeReason", s.getDisputeReason());
        result.put("disputedAt", s.getDisputedAt());
        result.put("version", s.getVersion());
        result.put("bookingStatus", b.getStatus());
        result.put("startTime", b.getStartTime());
        result.put("endTime", b.getEndTime());
        return result;
    }

    private Map<String,Object> view(Booking b, boolean teacherRole) {
        var result=new LinkedHashMap<String,Object>();
        result.put("id",b.getId());result.put("teacherId",b.getTeacherId());result.put("studentId",b.getStudentId());result.put("subjectId",b.getSubjectId());
        result.put("studentPackageId",b.getStudentPackageId());result.put("startTime",b.getStartTime());result.put("endTime",b.getEndTime());
        result.put("deliveryMode",b.getDeliveryMode());result.put("status",b.getStatus());result.put("trial",b.isTrial());
        result.put("outsideAvailabilityWarning",b.isOutsideAvailabilityWarning());result.put("version",b.getVersion());result.put("cancelReason",b.getCancelReason());
        result.put("meetingLink",b.getMeetingLink());result.put("locationAddress",b.getLocationAddress());
        var t=teachers.getTeacher(b.getTeacherId());var studentIdentity=identities.getIdentity(b.getStudentId());
        String teacherName = t == null || t.fullName() == null || t.fullName().isBlank()
                ? "Gia sư" : t.fullName();
        result.put("teacher",Map.of("id",b.getTeacherId(),"fullName",teacherName));
        result.put("student",Map.of("id",b.getStudentId(),"fullName",studentIdentity.map(i->i.fullName()).orElse("Học viên")));
        result.put("subject",Map.of("id",b.getSubjectId(),"name",subjects.getSubject(b.getSubjectId()).name()));
        result.put("sessionReport",reports.findByBookingId(b.getId()).map(r->{
            var v=new LinkedHashMap<String,Object>();v.put("content",r.getContent());v.put("feedback",r.getFeedback());v.put("followUpNote",r.getFollowUpNote());v.put("recordLink",r.getRecordLink());v.put("teacherSelfRating",r.getTeacherSelfRating());return v;
        }).orElse(null));
        if (!b.isTrial()) {
            settlements.findByBookingIdForRead(b.getId()).ifPresentOrElse(s -> {
                result.put("settlementStatus", s.getStatus());
                var sv = new LinkedHashMap<String,Object>();
                sv.put("status", s.getStatus());
                sv.put("teacherConfirmedAt", s.getTeacherConfirmedAt());
                sv.put("studentConfirmedAt", s.getStudentConfirmedAt());
                sv.put("confirmationDeadline", s.getInitialDeadline());
                sv.put("reopenDeadline", s.getReopenDeadline());
                sv.put("netAmountVnd", s.getNetAmountVnd());
                sv.put("disputeReason", s.getDisputeReason());
                sv.put("disputedAt", s.getDisputedAt());
                sv.put("version", s.getVersion());
                result.put("settlement", sv);
                Instant now = Instant.now(clock);
                Instant deadline = s.getStatus() == SettlementStatus.REOPENED ? s.getReopenDeadline() : s.getInitialDeadline();
                boolean open = (s.getStatus() == SettlementStatus.AWAITING_CONFIRMATION || s.getStatus() == SettlementStatus.REOPENED)
                        && deadline != null && now.isBefore(deadline) && b.getEndTime().isBefore(now);
                result.put("canConfirm", open && (teacherRole ? s.getTeacherConfirmedAt() == null : s.getStudentConfirmedAt() == null));
                result.put("canDispute", teacherRole && s.getStatus() == SettlementStatus.HELD);
                result.put("canReview", s.getStatus() == com.edtech.platform.booking.domain.SettlementStatus.RELEASED);
            }, () -> { result.put("settlementStatus", null); result.put("settlement", null); });
        } else {
            result.put("settlementStatus", null);
            result.put("settlement", null);
        }
        result.putIfAbsent("canConfirm", false);
        result.putIfAbsent("canDispute", false);
        result.putIfAbsent("canReview", b.isTrial() && b.getStatus() == BookingStatus.COMPLETED);
        return result;
    }
}

