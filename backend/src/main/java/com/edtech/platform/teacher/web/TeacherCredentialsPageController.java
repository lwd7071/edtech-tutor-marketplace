package com.edtech.platform.teacher.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.dto.TeacherCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherCredentialsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherCredentialsPageController.class);
    private static final DateTimeFormatter VERIFIED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'lúc' HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final TeacherCredentialService credentials;

    @GetMapping("/teacher/credentials")
    public String page(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) String edit,
                       Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "credentials");
        model.addAttribute("createForm", new CreateForm());
        List<TeacherCredentialView> rows = loadCredentials(user, model);
        UUID editingId = parseId(edit);
        TeacherCredentialView editing = rows.stream().filter(row -> row.id().equals(editingId)).findFirst().orElse(null);
        model.addAttribute("editingId", editing == null ? null : editing.id());
        model.addAttribute("editingApproved", editing != null && editing.status() == CredentialStatus.APPROVED);
        model.addAttribute("editForm", editing == null ? new EditForm() : new EditForm(editing.label(), editing.version()));
        if (edit != null && editing == null) model.addAttribute("pageError", "Không tìm thấy minh chứng cần chỉnh sửa. Danh sách đã được tải lại.");
        return "workspace/teacher-credentials";
    }

    @PostMapping("/teacher/credentials")
    public String create(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("createForm") CreateForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (form.getProof() == null || form.getProof().isEmpty()) bindingResult.rejectValue("proof", "required", "Chọn file minh chứng.");
        if (bindingResult.hasErrors()) {
            renderFormError(user, model, form, new EditForm(), null, "Kiểm tra tên và file minh chứng rồi thử lại.");
            return "workspace/teacher-credentials";
        }
        try {
            credentials.create(user.id(), form.getLabel(), form.getProof());
            redirect.addFlashAttribute("credentialNotice", "Đã gửi minh chứng để xét duyệt.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("credentialError", errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher credential creation failed");
            redirect.addFlashAttribute("credentialError", "Chưa gửi được minh chứng. Vui lòng thử lại.");
        }
        return "redirect:/teacher/credentials";
    }

    @PostMapping("/teacher/credentials/{id}/update")
    public String update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @Valid @ModelAttribute("editForm") EditForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (bindingResult.hasErrors()) {
            renderFormError(user, model, new CreateForm(), form, id, "Kiểm tra tên minh chứng rồi thử lại.");
            return "workspace/teacher-credentials";
        }
        try {
            credentials.update(user.id(), id, form.getLabel(), form.getProof(), form.getVersion());
            redirect.addFlashAttribute("credentialNotice", "Đã cập nhật minh chứng và gửi lại để xét duyệt.");
            return "redirect:/teacher/credentials";
        } catch (BusinessException ex) {
            renderFormError(user, model, new CreateForm(), form, id, errorMessage(ex.getErrorCode()));
            return "workspace/teacher-credentials";
        } catch (RuntimeException ex) {
            log.warn("Teacher credential update failed");
            renderFormError(user, model, new CreateForm(), form, id, "Chưa cập nhật được minh chứng. Tải lại nếu mục đã được thay đổi.");
            return "workspace/teacher-credentials";
        }
    }

    @PostMapping("/teacher/credentials/{id}/delete")
    public String delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam long version, RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        try {
            credentials.delete(user.id(), id, version);
            redirect.addFlashAttribute("credentialNotice", "Đã xóa minh chứng.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("credentialError", errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher credential delete failed");
            redirect.addFlashAttribute("credentialError", "Chưa xóa được minh chứng. Tải lại danh sách rồi thử lại.");
        }
        return "redirect:/teacher/credentials";
    }

    @GetMapping("/teacher/credentials/{id}/proof")
    public ResponseEntity<byte[]> proof(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable UUID id) throws IOException {
        requireTeacher(user);
        try {
            CredentialProofStorage.DownloadedFile file = credentials.proof(user.id(), id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(MediaType.parseMediaType(file.mimeType()))
                    .body(file.bytes());
        } catch (IOException ex) {
            log.warn("Teacher credential proof could not be loaded");
            return ResponseEntity.status(502)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Chưa mở được file minh chứng. Hãy quay lại trang và thử lại.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private List<TeacherCredentialView> loadCredentials(AuthenticatedUser user, Model model) {
        try {
            List<TeacherCredentialView> rows = credentials.list(user.id());
            model.addAttribute("credentials", rows.stream().map(row -> new CredentialRow(
                    row.id(), row.label(), row.status(), statusLabel(row.status()), row.rejectedReason(),
                    row.verifiedAt() == null ? null : VERIFIED_AT.format(row.verifiedAt()), row.version(),
                    "/teacher/credentials/" + row.id() + "/proof")).toList());
            model.addAttribute("credentialLimitReached", rows.size() >= 10);
            model.addAttribute("credentialsUnavailable", false);
            return rows;
        } catch (RuntimeException ex) {
            log.warn("Teacher credentials could not be loaded");
            model.addAttribute("credentials", List.of());
            model.addAttribute("credentialLimitReached", false);
            model.addAttribute("credentialsUnavailable", true);
            return List.of();
        }
    }

    private void renderFormError(AuthenticatedUser user, Model model, CreateForm createForm, EditForm editForm,
                                 UUID editingId, String message) {
        model.addAttribute("activeWorkspaceNav", "credentials");
        List<TeacherCredentialView> rows = loadCredentials(user, model);
        model.addAttribute("createForm", createForm);
        model.addAttribute("editForm", editForm);
        model.addAttribute("editingId", editingId);
        model.addAttribute("editingApproved", rows.stream().anyMatch(row -> row.id().equals(editingId) && row.status() == CredentialStatus.APPROVED));
        model.addAttribute("credentialFormError", message);
    }

    private String statusLabel(CredentialStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING -> "Đang chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Cần bổ sung";
        };
    }

    private String errorMessage(ErrorCode code) {
        if (code == ErrorCode.CONCURRENT_MODIFICATION) return "Minh chứng đã được cập nhật ở nơi khác. Kiểm tra lại dữ liệu và thử lại.";
        if (code == ErrorCode.CREDENTIAL_NOT_FOUND) return "Không tìm thấy minh chứng. Tải lại danh sách để kiểm tra.";
        if (code == ErrorCode.FORBIDDEN_RESOURCE) return "Bạn không có quyền thay đổi minh chứng này.";
        if (code == ErrorCode.CREDENTIAL_LIMIT_REACHED) return "Bạn đã đạt giới hạn 10 minh chứng.";
        if (code == ErrorCode.CREDENTIAL_PROOF_REQUIRED) return "Chọn file minh chứng trước khi gửi.";
        if (code == ErrorCode.FILE_TOO_LARGE) return "File không được vượt quá 5 MB.";
        if (code == ErrorCode.FILE_TYPE_NOT_ALLOWED) return "Chỉ nhận file JPG, PNG hoặc PDF đúng định dạng.";
        if (code == ErrorCode.FILE_UPLOAD_FAILED) return "Chưa lưu được file. Vui lòng thử lại sau.";
        if (code == ErrorCode.VALIDATION_ERROR) return "Tên minh chứng chưa hợp lệ.";
        return "Thao tác chưa thực hiện được. Vui lòng tải lại và thử lại.";
    }

    private UUID parseId(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ex) { return null; }
    }
    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record CredentialRow(UUID id, String label, CredentialStatus status, String statusLabel,
                                String rejectedReason, String verifiedAtLabel, long version, String proofPath) {}

    public static class CreateForm {
        @NotBlank(message = "Nhập tên chứng chỉ hoặc bằng cấp.") @Size(max = 120, message = "Tên tối đa 120 ký tự.")
        private String label = "";
        private MultipartFile proof;
        public String getLabel() { return label; } public void setLabel(String value) { label = value; }
        public MultipartFile getProof() { return proof; } public void setProof(MultipartFile value) { proof = value; }
    }

    public static class EditForm {
        @NotBlank(message = "Nhập tên chứng chỉ hoặc bằng cấp.") @Size(max = 120, message = "Tên tối đa 120 ký tự.")
        private String label = "";
        @NotNull(message = "Thiếu phiên bản minh chứng. Tải lại trang để tiếp tục.")
        private Long version;
        private MultipartFile proof;
        public EditForm() {}
        public EditForm(String label, Long version) { this.label = label; this.version = version; }
        public String getLabel() { return label; } public void setLabel(String value) { label = value; }
        public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
        public MultipartFile getProof() { return proof; } public void setProof(MultipartFile value) { proof = value; }
    }
}
