package com.edtech.platform.learning.web;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.StudentAssignmentProgress;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.StudentAssignmentDetail;
import com.edtech.platform.learning.service.StudentAssignmentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class StudentAssignmentPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentAssignmentPageController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);

    private final StudentAssignmentService assignments;
    private final AttachmentService attachments;
    private final Clock clock;

    @GetMapping("/student/assignments")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) String progress,
                       @RequestParam(defaultValue = "0") @Min(0) int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        StudentAssignmentProgress selected = parseProgress(progress);
        int safePage = Math.max(0, page);
        int safeSize = List.of(10, 20, 50, MAX_PAGE_SIZE).contains(size) ? size : DEFAULT_PAGE_SIZE;
        model.addAttribute("activeWorkspaceNav", "assignments");
        model.addAttribute("selectedProgress", selected.name());
        model.addAttribute("selectedProgressLabel", progressOptions().stream()
                .filter(option -> option.value().equals(selected.name())).findFirst().orElseThrow().label());
        model.addAttribute("progressInvalid", progress != null && !progress.isBlank()
                && !progress.toUpperCase(Locale.ROOT).equals(selected.name()));
        model.addAttribute("progressOptions", progressOptions());
        model.addAttribute("assignmentsUnavailable", false);
        model.addAttribute("pageSize", safeSize);
        try {
            Page<AssignmentDetail> result = assignments.getAssignments(user.id(), null, selected,
                    PageRequest.of(safePage, safeSize));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/student/assignments?progress=" + selected.name() + "&page="
                        + Math.max(0, result.getTotalPages() - 1) + "&size=" + safeSize;
            }
            model.addAttribute("assignmentRows", result.getContent().stream().map(row -> toRow(row, selected)).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Student assignment list is unavailable", ex);
            model.addAttribute("assignmentsUnavailable", true);
            model.addAttribute("assignmentRows", List.of());
            model.addAttribute("totalElements", 0L);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "student/assignments";
    }

    @GetMapping("/student/assignments/{id}")
    public String detail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                         Model model, HttpServletResponse response) {
        model.addAttribute("activeWorkspaceNav", "assignments");
        model.addAttribute("assignmentUnavailable", false);
        try {
            StudentAssignmentDetail detail = assignments.getDetail(user.id(), id);
            model.addAttribute("assignment", detail.assignment());
            model.addAttribute("submission", detail.submission());
            model.addAttribute("assignmentFiles", detail.assignmentAttachments());
            model.addAttribute("submissionFiles", detail.submissionAttachments());
            model.addAttribute("previousAnswer", textAnswer(detail));
            model.addAttribute("canEdit", canEdit(detail, clock.instant()));
            model.addAttribute("unsupportedQuiz", detail.assignment().getAssignmentType()
                    == com.edtech.platform.learning.domain.AssignmentType.SYSTEM_QUIZ);
            model.addAttribute("dueLabel", detail.assignment().getDueAt() == null ? "Chưa đặt" : DATE_TIME.format(detail.assignment().getDueAt()));
            model.addAttribute("assignmentOverdue", detail.assignment().getDueAt() != null
                    && clock.instant().isAfter(detail.assignment().getDueAt()));
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.ASSIGNMENT_NOT_FOUND
                    || ex.getErrorCode() == ErrorCode.FORBIDDEN_RESOURCE
                    || ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                model.addAttribute("assignmentUnavailable", true);
                return "student/assignment-detail";
            }
            log.warn("Student assignment detail could not be read", ex);
            model.addAttribute("assignmentUnavailable", true);
        } catch (RuntimeException ex) {
            log.warn("Student assignment detail is unavailable", ex);
            model.addAttribute("assignmentUnavailable", true);
        }
        return "student/assignment-detail";
    }

    @PostMapping("/student/assignments/{id}/submission")
    public String submit(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam(defaultValue = "0") long version,
                         @RequestParam String status,
                         @RequestParam(required = false) String answer,
                         @RequestParam(name = "files", required = false) List<MultipartFile> files,
                         RedirectAttributes redirect) {
        try {
            SubmissionStatus submissionStatus = SubmissionStatus.valueOf(status);
            if (submissionStatus != SubmissionStatus.DRAFT && submissionStatus != SubmissionStatus.SUBMITTED) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            StudentAssignmentDetail current = assignments.getDetail(user.id(), id);
            if (!canEdit(current, clock.instant())) {
                throw new BusinessException(current.assignment().getStatus() != AssignmentStatus.PUBLISHED
                        ? ErrorCode.ASSIGNMENT_INVALID_STATE
                        : ErrorCode.ASSIGNMENT_DUE_DATE_PASSED);
            }
            List<ContentBlock> blocks = submissionBlocks(answer, current, files, user.id());
            if (submissionStatus == SubmissionStatus.SUBMITTED && blocks.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Hãy nhập câu trả lời hoặc đính kèm tệp trước khi nộp.");
            }
            CreateSubmissionRequest request = new CreateSubmissionRequest();
            request.setVersion(version);
            request.setStatus(submissionStatus);
            request.setContentBlocks(blocks);
            assignments.createOrUpdateSubmission(user.id(), id, request);
            redirect.addFlashAttribute("submissionNotice", submissionStatus == SubmissionStatus.DRAFT
                    ? "Đã lưu bản nháp." : "Đã nộp bài cho gia sư.");
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.ASSIGNMENT_NOT_FOUND || ex.getErrorCode() == ErrorCode.FORBIDDEN_RESOURCE) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            redirect.addFlashAttribute("submissionAnswer", answer == null ? "" : answer);
            redirect.addFlashAttribute("submissionError", userMessage(ex));
        } catch (RuntimeException ex) {
            log.warn("Student submission could not be saved", ex);
            redirect.addFlashAttribute("submissionAnswer", answer == null ? "" : answer);
            redirect.addFlashAttribute("submissionError", "Chưa lưu được bài làm. Hãy tải lại trang rồi thử lại.");
        }
        return "redirect:/student/assignments/" + id;
    }

    private List<ContentBlock> submissionBlocks(String answer, StudentAssignmentDetail current,
                                                 List<MultipartFile> files, UUID studentId) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (answer != null && !answer.isBlank()) blocks.add(new ContentBlock("TEXT", answer.trim(), null, null, null));
        if (current.submission() != null && current.submission().getContentBlocks() != null) {
            current.submission().getContentBlocks().stream()
                    .filter(block -> block.getAttachmentId() != null)
                    .forEach(block -> blocks.add(new ContentBlock(block.getType(), null, block.getAttachmentId(), null, null)));
        }
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                AttachmentView uploaded = attachments.uploadAttachment(studentId, AttachableType.SUBMISSION, file);
                blocks.add(new ContentBlock("FILE", null, uploaded.getId(), null, uploaded.getOriginalFilename()));
            }
        }
        return blocks;
    }

    private boolean canEdit(StudentAssignmentDetail detail, Instant now) {
        return detail.assignment().getStatus() == AssignmentStatus.PUBLISHED
                && detail.assignment().getAssignmentType() == com.edtech.platform.learning.domain.AssignmentType.FREEFORM
                && (detail.assignment().getDueAt() == null || !now.isAfter(detail.assignment().getDueAt()))
                && (detail.submission() == null || detail.submission().getStatus() != SubmissionStatus.GRADED);
    }

    private String textAnswer(StudentAssignmentDetail detail) {
        if (detail.submission() == null || detail.submission().getContentBlocks() == null) return "";
        return detail.submission().getContentBlocks().stream().filter(block -> "TEXT".equals(block.getType()))
                .map(ContentBlock::getContent).filter(java.util.Objects::nonNull).findFirst().orElse("");
    }

    private StudentAssignmentProgress parseProgress(String value) {
        if (value == null || value.isBlank()) return StudentAssignmentProgress.TODO;
        try {
            return StudentAssignmentProgress.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return StudentAssignmentProgress.TODO;
        }
    }

    private AssignmentRow toRow(AssignmentDetail assignment, StudentAssignmentProgress progress) {
        boolean overdue = assignment.getDueAt() != null && clock.instant().isAfter(assignment.getDueAt());
        String dueLabel = assignment.getDueAt() == null ? "Chưa đặt hạn" : DATE_TIME.format(assignment.getDueAt());
        String assignmentStatusLabel = assignment.getStatus() == AssignmentStatus.CLOSED ? "Đã đóng" : "Đã giao";
        String progressLabel = switch (progress) {
            case TODO -> "Chưa làm";
            case SUBMITTED -> "Đã nộp";
            case GRADED -> "Đã chấm điểm";
        };
        return new AssignmentRow(assignment, dueLabel, overdue, progressLabel, assignmentStatusLabel);
    }

    private List<ProgressOption> progressOptions() {
        return List.of(new ProgressOption("TODO", "Chưa làm"),
                new ProgressOption("SUBMITTED", "Đã nộp"), new ProgressOption("GRADED", "Đã chấm điểm"));
    }

    private String userMessage(BusinessException ex) {
        return switch (ex.getErrorCode()) {
            case ASSIGNMENT_DUE_DATE_PASSED -> "Đã quá hạn nộp bài. Bạn vẫn có thể xem đề và bài đã lưu.";
            case ASSIGNMENT_INVALID_STATE -> "Bài tập đã đóng nên không thể thay đổi bài làm.";
            case CONCURRENT_MODIFICATION -> "Bài làm đã được cập nhật ở nơi khác. Hãy tải lại trang trước khi sửa tiếp.";
            case SUBMISSION_ALREADY_GRADED -> "Bài đã được chấm và không thể sửa.";
            case FILE_TOO_LARGE -> "Tệp vượt quá dung lượng cho phép (20 MB).";
            case FILE_TYPE_NOT_ALLOWED -> "Định dạng tệp này chưa được hỗ trợ.";
            case FILE_UPLOAD_FAILED -> "Chưa tải được tệp. Hãy thử lại.";
            default -> "Chưa lưu được bài làm. Hãy kiểm tra nội dung và thử lại.";
        };
    }

    public record AssignmentRow(AssignmentDetail data, String dueLabel, boolean overdue,
                                String progressLabel, String statusLabel) {}
    public record ProgressOption(String value, String label) {}
}
