package com.edtech.platform.admin.web;

import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectResolutionCommand;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class AdminSubjectsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminSubjectsPageController.class);
    private static final List<Integer> PAGE_SIZES = List.of(10, 20, 50);
    private static final Pattern SUBJECT_CODE = Pattern.compile("[A-Z0-9_-]+", Pattern.CASE_INSENSITIVE);
    private final AdminApprovalService approvals;
    private final SubjectFacade subjects;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/subjects")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        requireAdmin(user);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(size) ? size : 10;
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("proposalsUnavailable", false);
        model.addAttribute("subjectsUnavailable", false);
        model.addAttribute("educationLevels", educationLevels());
        try {
            Page<SubjectProposalSnapshot> result = approvals.subjectProposals(PageRequest.of(safePage, safeSize,
                    Sort.by(Sort.Direction.ASC, "createdAt", "id")));
            model.addAttribute("proposals", result.getContent().stream()
                    .map(proposal -> new ProposalRow(proposal, educationLevelLabel(proposal.educationLevel())))
                    .toList());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            if (result.getTotalPages() > 0 && safePage >= result.getTotalPages()) {
                return "redirect:/admin/subjects?page=" + (result.getTotalPages() - 1) + "&size=" + safeSize;
            }
        } catch (RuntimeException exception) {
            log.warn("Admin subject proposal queue is unavailable");
            model.addAttribute("proposals", List.of());
            model.addAttribute("totalElements", 0L);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("proposalsUnavailable", true);
        }
        try {
            model.addAttribute("activeSubjects", subjects.getActiveSubjects().stream()
                    .map(subject -> new ActiveSubjectOption(subject, educationLevelLabel(subject.educationLevel())))
                    .toList());
        } catch (RuntimeException exception) {
            log.warn("Active subject options for Admin approvals are unavailable");
            model.addAttribute("activeSubjects", List.of());
            model.addAttribute("subjectsUnavailable", true);
        }
        return "workspace/admin-subjects";
    }

    @PostMapping("/admin/subjects/{id}/link")
    public String linkExisting(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                               @RequestParam(required = false) String existingSubjectId,
                               @RequestParam(required = false, defaultValue = "") String note,
                               @RequestParam long version, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               HttpServletRequest request, RedirectAttributes redirect) {
        requireAdmin(user);
        String location = listLocation(page, size);
        if (note.length() > 1000) {
            redirect.addFlashAttribute("actionError", "Ghi chú tối đa 1.000 ký tự.");
            redirect.addFlashAttribute("linkProposalId", id.toString());
            redirect.addFlashAttribute("linkSubjectId", existingSubjectId);
            redirect.addFlashAttribute("linkNote", note);
            return "redirect:" + location;
        }
        UUID subjectId;
        try { subjectId = UUID.fromString(existingSubjectId); }
        catch (RuntimeException exception) {
            redirect.addFlashAttribute("actionError", "Chọn một môn học đang hoạt động.");
            redirect.addFlashAttribute("linkProposalId", id.toString());
            redirect.addFlashAttribute("linkSubjectId", existingSubjectId);
            redirect.addFlashAttribute("linkNote", note);
            return "redirect:" + location;
        }
        SubjectResolutionCommand command = new SubjectResolutionCommand(
                SubjectResolutionCommand.Resolution.LINK_EXISTING, subjectId, null, null, null, null,
                blankToNull(note), version);
        return approve(id, user, command, request, redirect, location, "Đã gắn đề xuất với môn học đang hoạt động.");
    }

    @PostMapping("/admin/subjects/{id}/create")
    public String createFromProposal(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                     @RequestParam(required = false) String code,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) String educationLevel,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false, defaultValue = "") String note,
                                     @RequestParam long version, @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     HttpServletRequest request, RedirectAttributes redirect) {
        requireAdmin(user);
        String location = listLocation(page, size);
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String normalizedName = name == null ? "" : name.trim();
        String normalizedLevel = educationLevel == null ? "" : educationLevel.trim();
        String normalizedDescription = description == null ? "" : description.trim();
        if (!SUBJECT_CODE.matcher(normalizedCode).matches() || normalizedCode.length() > 50
                || normalizedName.isBlank() || normalizedName.length() > 150
                || !isAllowedEducationLevel(normalizedLevel) || normalizedDescription.length() > 2000
                || note.length() > 1000) {
            redirect.addFlashAttribute("actionError", "Kiểm tra mã, tên, cấp học, mô tả và ghi chú của môn học.");
            redirect.addFlashAttribute("createProposalId", id.toString());
            redirect.addFlashAttribute("createForm", new CreateFormValues(code, name, educationLevel, description, note));
            return "redirect:" + location;
        }
        SubjectResolutionCommand command = new SubjectResolutionCommand(
                SubjectResolutionCommand.Resolution.CREATE_NEW, null, normalizedCode, normalizedName,
                normalizedLevel, blankToNull(normalizedDescription), blankToNull(note), version);
        return approve(id, user, command, request, redirect, location, "Đã phê duyệt và thêm môn học vào danh mục.");
    }

    @PostMapping("/admin/subjects/{id}/reject")
    public String reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         @RequestParam long version, @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size, HttpServletRequest request,
                         RedirectAttributes redirect) {
        requireAdmin(user);
        String location = listLocation(page, size);
        String cleanReason = reason.trim();
        if (cleanReason.isBlank()) {
            redirect.addFlashAttribute("actionError", "Nhập lý do từ chối đề xuất.");
            redirect.addFlashAttribute("rejectProposalId", id.toString());
            redirect.addFlashAttribute("rejectReason", reason);
            return "redirect:" + location;
        }
        try {
            approvals.rejectSubject(id, user.id(), cleanReason, version, auditContext(request));
            redirect.addFlashAttribute("actionSuccess", "Đã từ chối đề xuất môn học.");
        } catch (RuntimeException exception) {
            log.warn("Admin subject proposal rejection failed");
            redirect.addFlashAttribute("actionError", "Không thể xử lý đề xuất. Dữ liệu có thể đã được thay đổi; hãy tải lại danh sách.");
        }
        return "redirect:" + location;
    }

    private String approve(UUID id, AuthenticatedUser user, SubjectResolutionCommand command,
                           HttpServletRequest request, RedirectAttributes redirect, String location,
                           String successMessage) {
        try {
            approvals.approveSubject(id, user.id(), command, auditContext(request));
            redirect.addFlashAttribute("actionSuccess", successMessage);
        } catch (RuntimeException exception) {
            log.warn("Admin subject proposal approval failed");
            redirect.addFlashAttribute("actionError", "Không thể xử lý đề xuất. Kiểm tra lại mã môn hoặc tải danh sách mới nhất.");
            if (command.resolution() == SubjectResolutionCommand.Resolution.CREATE_NEW) {
                redirect.addFlashAttribute("createProposalId", id.toString());
                redirect.addFlashAttribute("createForm", new CreateFormValues(command.code(), command.name(),
                        command.educationLevel(), command.description(), command.note()));
            } else {
                redirect.addFlashAttribute("linkProposalId", id.toString());
                redirect.addFlashAttribute("linkSubjectId", command.existingSubjectId() == null
                        ? null : command.existingSubjectId().toString());
                redirect.addFlashAttribute("linkNote", command.note());
            }
        }
        return "redirect:" + location;
    }

    private String listLocation(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(size) ? size : 10;
        return "/admin/subjects?page=" + safePage + "&size=" + safeSize;
    }

    private AuditContext auditContext(HttpServletRequest request) {
        return new AuditContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private boolean isAllowedEducationLevel(String value) {
        return value.equals("ELEMENTARY") || value.equals("MIDDLE_SCHOOL") || value.equals("HIGH_SCHOOL")
                || value.equals("UNIVERSITY") || value.equals("OTHER");
    }

    private List<EducationOption> educationLevels() {
        return List.of(new EducationOption("ELEMENTARY", "Tiểu học"),
                new EducationOption("MIDDLE_SCHOOL", "THCS"),
                new EducationOption("HIGH_SCHOOL", "THPT"),
                new EducationOption("UNIVERSITY", "Đại học"),
                new EducationOption("OTHER", "Khác"));
    }

    private String educationLevelLabel(String value) {
        if (value == null) return "Chưa chọn cấp học";
        return switch (value) {
            case "ELEMENTARY", "PRIMARY_SCHOOL" -> "Tiểu học";
            case "MIDDLE_SCHOOL" -> "THCS";
            case "HIGH_SCHOOL" -> "THPT";
            case "UNIVERSITY" -> "Đại học";
            case "OTHER" -> "Khác";
            default -> value;
        };
    }

    public record ProposalRow(SubjectProposalSnapshot data, String educationLevelLabel) {}
    public record ActiveSubjectOption(SubjectSnapshot data, String educationLevelLabel) {
        public String displayLabel() { return data.code() + " · " + data.name() + " · " + educationLevelLabel; }
    }
    public record EducationOption(String value, String label) {}
    public record CreateFormValues(String code, String name, String educationLevel, String description, String note) {}
}
