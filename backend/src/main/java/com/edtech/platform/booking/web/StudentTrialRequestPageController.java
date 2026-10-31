package com.edtech.platform.booking.web;

import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentTrialRequestPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentTrialRequestPageController.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter LOCAL_INPUT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TrialRequestService trials;

    @PostMapping("/student/trials/requests")
    public String create(@AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam String teacherId,
                         @RequestParam String subjectId,
                         @RequestParam(required = false) String preferredStartTime,
                         @RequestParam(required = false) String note,
                         RedirectAttributes redirect) {
        UUID teacher = parseId(teacherId);
        if (teacher == null) {
            redirect.addFlashAttribute("trialRequestError", "Không tìm thấy hồ sơ gia sư để gửi yêu cầu.");
            return "redirect:/teachers";
        }
        String profileUrl = "/teachers/" + teacher + "#trial";
        UUID subject = parseId(subjectId);
        LocalDateTime preferredLocal = parseLocal(preferredStartTime);
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (subject == null || preferredLocal == null) {
            return retry(redirect, profileUrl, subjectId, preferredStartTime, cleanNote,
                    "Chọn môn học và thời gian mong muốn hợp lệ.");
        }
        if (cleanNote != null && cleanNote.length() > 1000) {
            return retry(redirect, profileUrl, subjectId, preferredStartTime, cleanNote.substring(0, 1000),
                    "Lời nhắn không được dài quá 1.000 ký tự.");
        }

        try {
            trials.create(user.id(), new CreateTrialRequest(teacher, subject,
                    preferredLocal.atZone(VIETNAM_ZONE).toInstant(), cleanNote));
            redirect.addFlashAttribute("trialRequestSuccess", "Đã gửi yêu cầu học thử. Gia sư sẽ phản hồi thời gian phù hợp.");
            return "redirect:/student/requests?tab=trial";
        } catch (BusinessException ex) {
            return retry(redirect, profileUrl, subjectId, preferredStartTime, cleanNote, publicError(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Student trial request could not be created");
            return retry(redirect, profileUrl, subjectId, preferredStartTime, cleanNote,
                    "Chưa gửi được yêu cầu học thử. Hãy thử lại.");
        }
    }

    private String retry(RedirectAttributes redirect, String profileUrl, String subjectId,
                         String preferredStartTime, String note, String error) {
        redirect.addFlashAttribute("trialRequestError", error);
        redirect.addFlashAttribute("trialFormSubjectId", subjectId);
        redirect.addFlashAttribute("trialFormPreferredStartTime", preferredStartTime);
        redirect.addFlashAttribute("trialFormNote", note);
        return "redirect:" + profileUrl;
    }

    private LocalDateTime parseLocal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDateTime.parse(value, LOCAL_INPUT); }
        catch (RuntimeException ex) { return null; }
    }

    private UUID parseId(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException ex) { return null; }
    }

    private String publicError(ErrorCode code) {
        return switch (code) {
            case TRIAL_REQUEST_ALREADY_PENDING -> "Bạn đã có một yêu cầu học thử đang chờ với gia sư này.";
            case TRIAL_ALREADY_USED -> "Bạn đã có buổi học thử với gia sư này.";
            case TEACHER_NOT_APPROVED -> "Gia sư hiện chưa thể nhận yêu cầu học thử.";
            case SUBJECT_NOT_ASSIGNED, SUBJECT_INACTIVE -> "Môn học này hiện không còn được gia sư mở dạy.";
            case BOOKING_IN_PAST -> "Thời gian mong muốn phải ở trong tương lai.";
            case ACCOUNT_NOT_ACTIVE -> "Tài khoản cần được kích hoạt trước khi gửi yêu cầu.";
            default -> "Chưa gửi được yêu cầu học thử. Hãy kiểm tra thông tin rồi thử lại.";
        };
    }
}
