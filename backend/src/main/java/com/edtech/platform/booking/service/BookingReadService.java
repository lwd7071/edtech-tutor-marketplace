package com.edtech.platform.booking.service;

import com.edtech.platform.booking.domain.*;
import com.edtech.platform.booking.repository.BookingRepository;
import com.edtech.platform.booking.repository.SessionReportRepository;
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

    @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(UUID userId, boolean teacherRole, BookingStatus status, Instant from, Instant to, Pageable pageable) {
        return (teacherRole ? bookings.findTeacher(teacherId(userId),status,from,to,pageable)
                : bookings.findStudent(userId,status,from,to,pageable)).map(this::view);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> detail(UUID userId, UUID id, boolean teacherRole) {
        Booking b=bookings.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (teacherRole ? !b.getTeacherId().equals(teacherId(userId)) : !b.getStudentId().equals(userId))
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return view(b);
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

    private Map<String,Object> view(Booking b) {
        var result=new LinkedHashMap<String,Object>();
        result.put("id",b.getId());result.put("teacherId",b.getTeacherId());result.put("studentId",b.getStudentId());result.put("subjectId",b.getSubjectId());
        result.put("studentPackageId",b.getStudentPackageId());result.put("startTime",b.getStartTime());result.put("endTime",b.getEndTime());
        result.put("deliveryMode",b.getDeliveryMode());result.put("status",b.getStatus());result.put("trial",b.isTrial());
        result.put("outsideAvailabilityWarning",b.isOutsideAvailabilityWarning());result.put("version",b.getVersion());result.put("cancelReason",b.getCancelReason());
        result.put("meetingLink",b.getMeetingLink());result.put("locationAddress",b.getLocationAddress());
        var t=teachers.getTeacher(b.getTeacherId());var s=identities.getIdentity(b.getStudentId());
        result.put("teacher",Map.of("id",b.getTeacherId(),"fullName",t==null?"Gia sư":t.fullName()));
        result.put("student",Map.of("id",b.getStudentId(),"fullName",s.map(i->i.fullName()).orElse("Học viên")));
        result.put("subject",Map.of("id",b.getSubjectId(),"name",subjects.getSubject(b.getSubjectId()).name()));
        result.put("sessionReport",reports.findByBookingId(b.getId()).map(r->{
            var v=new LinkedHashMap<String,Object>();v.put("content",r.getContent());v.put("feedback",r.getFeedback());v.put("followUpNote",r.getFollowUpNote());v.put("recordLink",r.getRecordLink());v.put("teacherSelfRating",r.getTeacherSelfRating());return v;
        }).orElse(null));
        return result;
    }
}

