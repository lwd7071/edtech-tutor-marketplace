package com.edtech.platform.admin.web;

import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import com.edtech.platform.teacher.service.TeacherDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminTeacherApprovalsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminTeacherApprovalsPageController.class);
    private static final List<Integer> PAGE_SIZES = List.of(10, 20, 50);
    private final AdminApprovalService approvals;
    private final TeacherDocumentService documents;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/teachers")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "PENDING_APPROVAL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        requireAdmin(user);
        ProfileStatus selected = parseStatus(status);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(size) ? size : 20;
        model.addAttribute("activeWorkspaceNav", "teachers");
        model.addAttribute("selectedStatus", selected.name());
        model.addAttribute("statusOptions", Arrays.stream(ProfileStatus.values()).map(s -> new StatusOption(s.name(), statusLabel(s))).toList());
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("pageUnavailable", false);
        model.addAttribute("pageTitle", statusLabel(selected));
        try {
            Page<TeacherApprovalSnapshot> result = approvals.teacherApprovals(selected.name(),
                    PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt", "id")));
            model.addAttribute("approvals", result.getContent());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            if (result.getTotalPages() > 0 && safePage >= result.getTotalPages()) {
                return "redirect:/admin/teachers?status=" + selected.name() + "&page=" + (result.getTotalPages() - 1) + "&size=" + safeSize;
            }
        } catch (RuntimeException exception) {
            log.warn("Admin teacher approval queue is unavailable");
            model.addAttribute("approvals", List.of());
            model.addAttribute("totalElements", 0L);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("pageUnavailable", true);
        }
        return "workspace/admin-teachers";
    }

    @PostMapping("/admin/teachers/{id}/approve")
    public String approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam(required = false, defaultValue = "") String note,
                          @RequestParam(defaultValue = "PENDING_APPROVAL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          HttpServletRequest request, RedirectAttributes redirect) {
        requireAdmin(user);
        String returnQuery = returnQuery(status, page, size);
        if (note.length() > 1000) {
            redirect.addFlashAttribute("actionError", "Ghi chú tối đa 1.000 ký tự.");
            return "redirect:/admin/teachers" + returnQuery;
        }
        try {
            approvals.approveTeacher(id, user.id(), note, auditContext(request));
            redirect.addFlashAttribute("actionSuccess", "Đã duyệt hồ sơ gia sư.");
        } catch (RuntimeException exception) {
            log.warn("Admin teacher approval action failed");
            redirect.addFlashAttribute("actionError", "Không thể duyệt hồ sơ. Danh sách đã được tải lại để kiểm tra trạng thái mới nhất.");
        }
        return "redirect:/admin/teachers" + returnQuery;
    }

    @PostMapping("/admin/teachers/{id}/reject")
    public String reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         @RequestParam(defaultValue = "PENDING_APPROVAL") String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         HttpServletRequest request, RedirectAttributes redirect) {
        requireAdmin(user);
        String returnQuery = returnQuery(status, page, size);
        String cleanReason = reason.trim();
        if (cleanReason.isBlank() || cleanReason.length() > 1000) {
            redirect.addFlashAttribute("actionError", cleanReason.isBlank()
                    ? "Nhập lý do từ chối hồ sơ." : "Lý do tối đa 1.000 ký tự.");
            redirect.addFlashAttribute("rejectedTeacherId", id.toString());
            redirect.addFlashAttribute("rejectedReason", reason);
            return "redirect:/admin/teachers" + returnQuery;
        }
        try {
            approvals.rejectTeacher(id, user.id(), cleanReason, auditContext(request));
            redirect.addFlashAttribute("actionSuccess", "Đã từ chối hồ sơ gia sư.");
        } catch (RuntimeException exception) {
            log.warn("Admin teacher rejection action failed");
            redirect.addFlashAttribute("actionError", "Không thể từ chối hồ sơ. Danh sách đã được tải lại để kiểm tra trạng thái mới nhất.");
        }
        return "redirect:/admin/teachers" + returnQuery;
    }

    @GetMapping("/admin/teachers/documents/{id}/proof")
    public ResponseEntity<byte[]> proof(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        requireAdmin(user);
        try {
            var file = documents.adminProof(id);
            MediaType mediaType = switch (file.mimeType() == null ? "" : file.mimeType().toLowerCase()) {
                case "application/pdf" -> MediaType.APPLICATION_PDF;
                case "image/jpeg" -> MediaType.IMAGE_JPEG;
                case "image/png" -> MediaType.IMAGE_PNG;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teacher-proof")
                    .contentType(mediaType)
                    .body(file.bytes());
        } catch (BusinessException exception) {
            log.warn("Admin teacher proof could not be loaded");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (java.io.IOException exception) {
            log.warn("Admin teacher proof storage is unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
    }

    private String returnQuery(String rawStatus, int page, int size) {
        ProfileStatus selected = parseStatus(rawStatus);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(size) ? size : 20;
        return "?status=" + selected.name() + "&page=" + safePage + "&size=" + safeSize;
    }

    private ProfileStatus parseStatus(String value) {
        try { return ProfileStatus.valueOf(value); }
        catch (RuntimeException ignored) { return ProfileStatus.PENDING_APPROVAL; }
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    private AuditContext auditContext(HttpServletRequest request) {
        return new AuditContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    private String statusLabel(ProfileStatus status) {
        return switch (status) {
            case PENDING_APPROVAL -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
            case DRAFT -> "Bản nháp";
        };
    }

    public record StatusOption(String value, String label) {}
}
