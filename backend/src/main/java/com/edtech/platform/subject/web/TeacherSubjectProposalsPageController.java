package com.edtech.platform.subject.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.service.SubjectProposalService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class TeacherSubjectProposalsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherSubjectProposalsPageController.class);
    private static final int PAGE_SIZE = 10;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);
    private final SubjectProposalService proposals;

    @GetMapping("/teacher/subject-proposals")
    public String page(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "subject-proposals");
        model.addAttribute("proposalForm", new ProposalForm());
        return render(user, status, page, model);
    }

    @PostMapping("/teacher/subject-proposals")
    public String create(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("proposalForm") ProposalForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeWorkspaceNav", "subject-proposals");
            model.addAttribute("proposalFormError", "Kiểm tra tên môn và cấp học trước khi gửi.");
            render(user, "ALL", 0, model);
            return "workspace/teacher-subject-proposals";
        }
        try {
            proposals.createProposal(user.id(), new CreateSubjectProposalRequest(
                    form.getProposedName().trim(), form.getEducationLevel(), clean(form.getDescription())));
            redirect.addFlashAttribute("proposalNotice", "Đã gửi đề xuất môn học để xét duyệt.");
        } catch (BusinessException ex) {
            render(user, "ALL", 0, model);
            model.addAttribute("proposalFormError", errorMessage(ex.getErrorCode()));
            return "workspace/teacher-subject-proposals";
        } catch (RuntimeException ex) {
            log.warn("Teacher subject proposal creation failed");
            render(user, "ALL", 0, model);
            model.addAttribute("proposalFormError", "Chưa gửi được đề xuất. Tải lại trang rồi thử lại.");
            return "workspace/teacher-subject-proposals";
        }
        return "redirect:/teacher/subject-proposals?status=ALL&page=0";
    }

    private String render(AuthenticatedUser user, String rawStatus, int requestedPage, Model model) {
        ProposalStatus filter = parseStatus(rawStatus);
        int safePage = Math.max(0, Math.min(requestedPage, 100_000));
        model.addAttribute("status", filter == null ? "ALL" : filter.name());
        model.addAttribute("statusInvalid", rawStatus != null && !"ALL".equalsIgnoreCase(rawStatus.trim()) && filter == null);
        model.addAttribute("educationLevels", educationLevels());
        model.addAttribute("proposalsUnavailable", false);
        try {
            Page<SubjectProposalView> result = proposals.getProposals(user.id(), filter,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/teacher/subject-proposals?status=" + (filter == null ? "ALL" : filter.name())
                        + "&page=" + (result.getTotalPages() - 1);
            }
            model.addAttribute("proposalRows", result.getContent().stream()
                    .map(item -> new ProposalRow(item, statusLabel(item.status()), educationLevelLabel(item.educationLevel()),
                            item.reviewedAt() == null ? null : DATE_TIME.format(item.reviewedAt())))
                    .toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Teacher subject proposals could not be loaded");
            model.addAttribute("proposalsUnavailable", true);
            model.addAttribute("proposalRows", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "workspace/teacher-subject-proposals";
    }

    private ProposalStatus parseStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) return null;
        try { return ProposalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private List<LevelOption> educationLevels() {
        return List.of(EducationLevel.values()).stream().filter(level -> level != EducationLevel.PRIMARY_SCHOOL)
                .map(level -> new LevelOption(level, educationLevelLabel(level)))
                .toList();
    }

    private String educationLevelLabel(EducationLevel level) {
        if (level == null) return "Cấp học đang cập nhật";
        return switch (level) {
            case ELEMENTARY, PRIMARY_SCHOOL -> "Tiểu học";
            case MIDDLE_SCHOOL -> "Trung học cơ sở";
            case HIGH_SCHOOL -> "Trung học phổ thông";
            case UNIVERSITY -> "Đại học";
            case OTHER -> "Khác";
        };
    }

    private String statusLabel(ProposalStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING -> "Đang chờ duyệt";
            case APPROVED -> "Đã chấp thuận";
            case REJECTED -> "Cần chỉnh sửa";
        };
    }

    private String errorMessage(ErrorCode code) {
        if (code == ErrorCode.TEACHER_PROFILE_NOT_FOUND) return "Chưa tìm thấy hồ sơ gia sư của bạn.";
        if (code == ErrorCode.VALIDATION_ERROR) return "Tên hoặc mô tả đề xuất chưa hợp lệ.";
        return "Chưa gửi được đề xuất. Vui lòng tải lại trang rồi thử lại.";
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record LevelOption(EducationLevel value, String label) {}
    public record ProposalRow(SubjectProposalView data, String statusLabel, String educationLevelLabel, String reviewedAtLabel) {}

    public static class ProposalForm {
        @NotBlank(message = "Nhập tên môn học.")
        @Pattern(regexp = "^[^<]*$", message = "Tên môn không được chứa ký tự <.")
        private String proposedName = "";
        @NotNull(message = "Chọn cấp học.")
        private EducationLevel educationLevel;
        @Pattern(regexp = "^[^<]*$", message = "Mô tả không được chứa ký tự <.")
        private String description;
        public String getProposedName() { return proposedName; }
        public void setProposedName(String value) { proposedName = value; }
        public EducationLevel getEducationLevel() { return educationLevel; }
        public void setEducationLevel(EducationLevel value) { educationLevel = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
    }
}
