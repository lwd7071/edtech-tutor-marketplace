package com.edtech.platform.teacher.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.domain.VerificationStatus;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.service.TeacherDocumentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherDocumentsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherDocumentsPageController.class);
    private static final DateTimeFormatter VERIFIED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'lúc' HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final TeacherDocumentService documents;

    @GetMapping("/teacher/documents")
    public String page(@AuthenticationPrincipal AuthenticatedUser user, Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "documents");
        model.addAttribute("uploadForm", new UploadForm());
        loadDocuments(user, model);
        return "workspace/teacher-documents";
    }

    @PostMapping("/teacher/documents/upload")
    public String upload(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("uploadForm") UploadForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (bindingResult.hasErrors()) {
            model.addAttribute("uploadHasErrors", true);
            model.addAttribute("uploadError", "Chọn tệp hợp lệ và kiểm tra tên tài liệu.");
            model.addAttribute("activeWorkspaceNav", "documents");
            loadDocuments(user, model);
            return "workspace/teacher-documents";
        }
        try {
            documents.uploadDocument(user.id(), form.getFile(), DocumentType.IDENTITY, blankToNull(form.getTitle()));
            redirect.addFlashAttribute("documentsNotice", "Đã tải giấy tờ lên để xác minh.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("documentsError", uploadMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher identity document upload failed");
            redirect.addFlashAttribute("documentsError", "Chưa tải được giấy tờ. Vui lòng thử lại.");
        }
        return "redirect:/teacher/documents";
    }

    @PostMapping("/teacher/documents/{id}/delete")
    public String delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        try {
            documents.deleteDocument(user.id(), id);
            redirect.addFlashAttribute("documentsNotice", "Đã xóa tài liệu.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("documentsError", deleteMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher identity document delete failed");
            redirect.addFlashAttribute("documentsError", "Chưa xóa được tài liệu. Tải lại danh sách rồi thử lại.");
        }
        return "redirect:/teacher/documents";
    }

    private void loadDocuments(AuthenticatedUser user, Model model) {
        try {
            List<TeacherDocumentView> rows = documents.getDocuments(user.id());
            model.addAttribute("documents", rows.stream().map(row -> new DocumentRow(
                    row.id(), safeTitle(row.title()), statusLabel(row.verificationStatus()), row.verificationStatus(),
                    row.fileSize() == null || row.fileSize() < 0 ? "Dung lượng chưa rõ" : formatSize(row.fileSize()),
                    row.verificationStatus() == VerificationStatus.VERIFIED && row.verifiedAt() != null
                            ? VERIFIED_AT.format(row.verifiedAt()) : null,
                    row.verificationStatus() != VerificationStatus.VERIFIED
            )).toList());
            model.addAttribute("documentsUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher identity documents could not be loaded");
            model.addAttribute("documents", List.of());
            model.addAttribute("documentsUnavailable", true);
        }
    }

    private String uploadMessage(ErrorCode code) {
        if (code == ErrorCode.FILE_TOO_LARGE) return "Tệp vượt quá giới hạn 10 MB.";
        if (code == ErrorCode.FILE_TYPE_NOT_ALLOWED) return "Chỉ nhận tệp PDF, PNG hoặc JPG/JPEG.";
        if (code == ErrorCode.TEACHER_PROFILE_NOT_FOUND) return "Chưa tìm thấy hồ sơ gia sư để lưu giấy tờ.";
        if (code == ErrorCode.FILE_UPLOAD_FAILED) return "Dịch vụ lưu tệp chưa sẵn sàng. Vui lòng thử lại sau.";
        return "Tệp chưa hợp lệ. Hãy kiểm tra định dạng và thử lại.";
    }

    private String deleteMessage(ErrorCode code) {
        if (code == ErrorCode.TEACHER_DOCUMENT_NOT_DELETABLE) return "Tài liệu đã xác minh nên không thể xóa.";
        if (code == ErrorCode.RESOURCE_NOT_FOUND) return "Không tìm thấy tài liệu. Tải lại danh sách để kiểm tra.";
        if (code == ErrorCode.FORBIDDEN_RESOURCE) return "Bạn không có quyền xóa tài liệu này.";
        return "Chưa xóa được tài liệu. Tải lại danh sách rồi thử lại.";
    }

    private String statusLabel(VerificationStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING -> "Đang chờ xác minh";
            case VERIFIED -> "Đã xác minh";
            case REJECTED -> "Cần gửi lại";
        };
    }
    private String safeTitle(String title) { return title == null || title.isBlank() ? "Giấy tờ định danh" : title; }
    private String formatSize(long bytes) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(1);
        return bytes >= 1024 * 1024 ? format.format((double) bytes / (1024 * 1024)) + " MB" : format.format((double) bytes / 1024) + " KB";
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void noStore(HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record DocumentRow(UUID id, String title, String statusLabel, VerificationStatus status,
                              String fileSizeLabel, String verifiedAtLabel, boolean deletable) {}

    public static class UploadForm {
        @NotNull(message = "Chọn một tệp giấy tờ.") private MultipartFile file;
        @Size(max = 255, message = "Tên tài liệu tối đa 255 ký tự.")
        private String title;
        public MultipartFile getFile() { return file; }
        public void setFile(MultipartFile file) { this.file = file; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }
}
