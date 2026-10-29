package com.edtech.platform.admin.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.dto.AdminCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminCredentialsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminCredentialsPageController.class);
    private final TeacherCredentialService credentials;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/credentials")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "PENDING") String status,
                       Model model) {
        requireAdmin(user);
        CredentialStatus selected = parseStatus(status);
        model.addAttribute("selectedStatus", selected.name());
        model.addAttribute("statusLabel", statusLabel(selected));
        model.addAttribute("credentialsUnavailable", false);
        try {
            List<AdminCredentialView> rows = credentials.adminList(selected.name());
            model.addAttribute("credentials", rows);
            model.addAttribute("credentialCount", rows.size());
        } catch (RuntimeException exception) {
            log.warn("Admin credential queue is unavailable");
            model.addAttribute("credentials", List.of());
            model.addAttribute("credentialCount", 0);
            model.addAttribute("credentialsUnavailable", true);
        }
        return "workspace/admin-credentials";
    }

    @PostMapping("/admin/credentials/{id}/approve")
    public String approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam long version, @RequestParam(defaultValue = "PENDING") String status,
                          RedirectAttributes redirect) {
        requireAdmin(user);
        CredentialStatus selected = parseStatus(status);
        try {
            credentials.approve(id, user.id(), version);
            redirect.addFlashAttribute("actionSuccess", "Đã duyệt minh chứng năng lực.");
        } catch (RuntimeException exception) {
            log.warn("Admin credential approval failed");
            redirect.addFlashAttribute("actionError", "Không thể duyệt minh chứng. Danh sách đã được tải lại để kiểm tra trạng thái mới nhất.");
        }
        return "redirect:/admin/credentials?status=" + selected.name();
    }

    @PostMapping("/admin/credentials/{id}/reject")
    public String reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         @RequestParam long version, @RequestParam(defaultValue = "PENDING") String status,
                         RedirectAttributes redirect) {
        requireAdmin(user);
        CredentialStatus selected = parseStatus(status);
        String cleanReason = reason.trim();
        if (cleanReason.isBlank() || cleanReason.length() > 1000) {
            redirect.addFlashAttribute("actionError", cleanReason.isBlank()
                    ? "Nhập lý do từ chối minh chứng." : "Lý do từ chối tối đa 1.000 ký tự.");
            redirect.addFlashAttribute("rejectedCredentialId", id.toString());
            redirect.addFlashAttribute("rejectedReason", reason);
            return "redirect:/admin/credentials?status=" + selected.name();
        }
        try {
            credentials.reject(id, cleanReason, user.id(), version);
            redirect.addFlashAttribute("actionSuccess", "Đã từ chối minh chứng năng lực.");
        } catch (RuntimeException exception) {
            log.warn("Admin credential rejection failed");
            redirect.addFlashAttribute("actionError", "Không thể từ chối minh chứng. Danh sách đã được tải lại để kiểm tra trạng thái mới nhất.");
        }
        return "redirect:/admin/credentials?status=" + selected.name();
    }

    @GetMapping("/admin/credentials/{id}/proof")
    public ResponseEntity<byte[]> proof(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        requireAdmin(user);
        try {
            var file = credentials.adminProof(id);
            MediaType mediaType = switch (file.mimeType() == null ? "" : file.mimeType().toLowerCase()) {
                case "application/pdf" -> MediaType.APPLICATION_PDF;
                case "image/jpeg" -> MediaType.IMAGE_JPEG;
                case "image/png" -> MediaType.IMAGE_PNG;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=credential-proof")
                    .contentType(mediaType)
                    .body(file.bytes());
        } catch (BusinessException exception) {
            log.warn("Admin credential proof could not be loaded");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (IOException exception) {
            log.warn("Admin credential proof storage is unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }
    }

    private CredentialStatus parseStatus(String value) {
        try { return CredentialStatus.valueOf(value); }
        catch (RuntimeException ignored) { return CredentialStatus.PENDING; }
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    private String statusLabel(CredentialStatus status) {
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
        };
    }
}
