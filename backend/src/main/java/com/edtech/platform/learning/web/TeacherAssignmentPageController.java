package com.edtech.platform.learning.web;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.enrollment.service.TeacherLearnerService;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.AssignmentType;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateAssignmentRequest;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.learning.service.TeacherAssignmentService;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class TeacherAssignmentPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherAssignmentPageController.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private static final int PAGE_SIZE = 20;

    private final AssignmentReadService reads;
    private final TeacherAssignmentService commands;
    private final TeacherLearnerService learners;
    private final TeacherSubjectService teacherSubjects;
    private final ObjectMapper objectMapper;
    private final AttachmentService attachmentService;
    private final AttachmentFacade attachmentFacade;
    private final Clock clock;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/teacher/assignments")
    public String list(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(defaultValue = "0") @Min(0) int page, Model model) {
        int safePage = Math.max(0, page);
        model.addAttribute("activeWorkspaceNav", "assignments");
        model.addAttribute("assignmentsUnavailable", false);
        try {
            Page<Map<String, Object>> result = reads.teacherList(user.id(), PageRequest.of(safePage, PAGE_SIZE,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/teacher/assignments?page=" + Math.max(0, result.getTotalPages() - 1);
            }
            model.addAttribute("assignmentRows", result.getContent().stream().map(this::listRow).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Teacher assignment list is unavailable", ex);
            model.addAttribute("assignmentsUnavailable", true);
            model.addAttribute("assignmentRows", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "workspace/teacher-assignments";
    }

    @GetMapping("/teacher/assignments/new")
    public String createForm(@AuthenticationPrincipal AuthenticatedUser user,
                             @RequestParam(defaultValue = "0") @Min(0) int learnerPage, Model model) {
        model.addAttribute("activeWorkspaceNav", "assignments");
        if (!model.containsAttribute("assignmentForm")) model.addAttribute("assignmentForm", new AssignmentForm(null, null, "", "", "", 0L));
        if (!model.containsAttribute("assignmentError")) model.addAttribute("assignmentError", null);
        int safeLearnerPage = Math.max(0, learnerPage);
        try {
            var page = learners.list(user.id(), null, PageRequest.of(safeLearnerPage, 50));
            Map<UUID, String> studentOptions = new LinkedHashMap<>();
            page.forEach(row -> studentOptions.putIfAbsent(row.studentId(), row.studentName()));
            model.addAttribute("studentOptions", studentOptions.entrySet().stream().map(e -> new StudentOption(e.getKey(), e.getValue())).toList());
            model.addAttribute("subjectOptions", teacherSubjects.getSubjects(user.id()).stream()
                    .filter(row -> row.isActive())
                    .map(row -> new SubjectOption(row.subject().id(), row.subject().name())).toList());
            model.addAttribute("learnerPage", page.getNumber());
            model.addAttribute("learnerTotalPages", page.getTotalPages());
            model.addAttribute("hasLearnerPrevious", page.hasPrevious());
            model.addAttribute("hasLearnerNext", page.hasNext());
            model.addAttribute("assignmentFormUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher assignment form options are unavailable", ex);
            model.addAttribute("studentOptions", List.of());
            model.addAttribute("subjectOptions", List.of());
            model.addAttribute("learnerPage", safeLearnerPage);
            model.addAttribute("learnerTotalPages", 0);
            model.addAttribute("hasLearnerPrevious", false);
            model.addAttribute("hasLearnerNext", false);
            model.addAttribute("assignmentFormUnavailable", true);
        }
        return "workspace/teacher-assignment-form";
    }

    @PostMapping("/teacher/assignments")
    public String create(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam UUID studentId,
                         @RequestParam UUID subjectId, @RequestParam String title,
                         @RequestParam(required = false) String dueAt, @RequestParam(required = false) String content,
                         @RequestParam(required = false) List<MultipartFile> files, @RequestParam String action,
                         @RequestParam(defaultValue = "0") int learnerPage,
                         RedirectAttributes redirect) {
        try {
            AssignmentStatus status = "publish".equals(action) ? AssignmentStatus.PUBLISHED : AssignmentStatus.DRAFT;
            commands.createAssignment(user.id(), assignmentRequest(user.id(), 0L, studentId, subjectId, title, dueAt, content, files, status));
            redirect.addFlashAttribute("assignmentNotice", status == AssignmentStatus.PUBLISHED ? "Đã giao bài cho học viên." : "Đã lưu bài tập nháp.");
            return "redirect:/teacher/assignments";
        } catch (RuntimeException ex) {
            log.warn("Teacher assignment could not be created", ex);
            redirect.addFlashAttribute("assignmentError", publicMessage(ex));
            redirect.addFlashAttribute("assignmentForm", new AssignmentForm(studentId, subjectId, title, dueAt, content, 0L));
            redirect.addAttribute("learnerPage", Math.max(0, learnerPage));
            return "redirect:/teacher/assignments/new#assignment-action-error";
        }
    }

    @GetMapping("/teacher/assignments/{id}")
    public String detail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "assignments");
        try {
            Map<String, Object> assignment = reads.detail(user.id(), id, true);
            model.addAttribute("assignment", assignment);
            model.addAttribute("contentBlocks", contentBlocks(assignment));
            model.addAttribute("assignmentForm", formFrom(assignment));
            model.addAttribute("assignmentFiles", attachments(assignment));
            model.addAttribute("submissionRow", submissionRow(assignment));
            model.addAttribute("assignmentUnavailable", false);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.ASSIGNMENT_NOT_FOUND || ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND
                    || ex.getErrorCode() == ErrorCode.FORBIDDEN_RESOURCE) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            log.warn("Teacher assignment detail could not be read", ex);
            model.addAttribute("assignmentUnavailable", true);
        } catch (RuntimeException ex) {
            log.warn("Teacher assignment detail is unavailable", ex);
            model.addAttribute("assignmentUnavailable", true);
        }
        return "workspace/teacher-assignment-detail";
    }

    @PostMapping("/teacher/assignments/{id}/draft")
    public String updateDraft(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                              @RequestParam long version, @RequestParam String title,
                              @RequestParam(required = false) String dueAt, @RequestParam(required = false) String content,
                              @RequestParam(required = false) List<MultipartFile> files, RedirectAttributes redirect) {
        boolean failed = false;
        try {
            Map<String, Object> current = reads.detail(user.id(), id, true);
            UUID studentId = (UUID) current.get("studentId");
            UUID subjectId = (UUID) current.get("subjectId");
            List<ContentBlock> blocks = blocks(content, files, user.id(), attachmentIds(current));
            commands.updateDraft(user.id(), id, request(version, studentId, subjectId, title, instant(dueAt), blocks));
            redirect.addFlashAttribute("assignmentNotice", "Đã lưu thay đổi.");
        } catch (RuntimeException ex) {
            failed = true;
            log.warn("Teacher assignment draft could not be updated", ex);
            redirect.addFlashAttribute("assignmentError", publicMessage(ex));
        }
        return "redirect:/teacher/assignments/" + id + (failed ? "#assignment-action-error" : "");
    }

    @PostMapping("/teacher/assignments/{id}/publish")
    public String publish(@PathVariable UUID id, @RequestParam long version, @AuthenticationPrincipal AuthenticatedUser user,
                          RedirectAttributes redirect) {
        return transition(id, version, user, redirect, true);
    }

    @PostMapping("/teacher/assignments/{id}/close")
    public String close(@PathVariable UUID id, @RequestParam long version, @AuthenticationPrincipal AuthenticatedUser user,
                        RedirectAttributes redirect) {
        return transition(id, version, user, redirect, false);
    }

    @GetMapping("/teacher/submissions/{id}")
    public String submission(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "assignments");
        try {
            SubmissionDetail submission = reads.submission(user.id(), id);
            Map<String, Object> assignment = reads.detail(user.id(), submission.getAssignmentId(), true);
            model.addAttribute("submission", submission);
            model.addAttribute("assignment", assignment);
            model.addAttribute("submissionFiles", attachmentViews(submission.getContentBlocks()));
            model.addAttribute("submissionUnavailable", false);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.SUBMISSION_NOT_FOUND || ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND
                    || ex.getErrorCode() == ErrorCode.FORBIDDEN_RESOURCE) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            log.warn("Teacher submission could not be read", ex);
            model.addAttribute("submissionUnavailable", true);
        } catch (RuntimeException ex) {
            log.warn("Teacher submission is unavailable", ex);
            model.addAttribute("submissionUnavailable", true);
        }
        return "workspace/teacher-submission-detail";
    }

    @PostMapping("/teacher/submissions/{id}/grade")
    public String grade(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                        @RequestParam long version, @RequestParam BigDecimal score,
                        @RequestParam(required = false) String feedbackText, RedirectAttributes redirect) {
        boolean failed = false;
        try {
            GradeSubmissionRequest request = new GradeSubmissionRequest();
            request.setVersion(version);
            request.setScore(score);
            request.setFeedbackText(feedbackText);
            commands.gradeSubmission(user.id(), id, request);
            redirect.addFlashAttribute("gradeNotice", "Đã lưu kết quả chấm.");
        } catch (RuntimeException ex) {
            failed = true;
            log.warn("Teacher submission could not be graded", ex);
            redirect.addFlashAttribute("gradeError", publicMessage(ex));
        }
        return "redirect:/teacher/submissions/" + id + (failed ? "#grade-action-error" : "");
    }

    private String transition(UUID id, long version, AuthenticatedUser user, RedirectAttributes redirect, boolean publish) {
        boolean failed = false;
        try {
            commands.transition(user.id(), id, publish ? AssignmentStatus.PUBLISHED : AssignmentStatus.CLOSED, version);
            redirect.addFlashAttribute("assignmentNotice", publish ? "Đã giao bài cho học viên." : "Đã đóng bài tập.");
        } catch (RuntimeException ex) {
            failed = true;
            log.warn("Teacher assignment status could not be changed", ex);
            redirect.addFlashAttribute("assignmentError", publicMessage(ex));
        }
        return "redirect:/teacher/assignments/" + id + (failed ? "#assignment-action-error" : "");
    }

    private CreateAssignmentRequest assignmentRequest(UUID ownerId, long version, UUID studentId, UUID subjectId, String title,
                                                       String dueAt, String content, List<MultipartFile> files, AssignmentStatus status) {
        return request(version, studentId, subjectId, title, instant(dueAt), blocks(content, files, ownerId, List.of()), status);
    }

    private CreateAssignmentRequest request(long version, UUID studentId, UUID subjectId, String title, Instant dueAt, List<ContentBlock> blocks) {
        return request(version, studentId, subjectId, title, dueAt, blocks, AssignmentStatus.DRAFT);
    }

    private CreateAssignmentRequest request(long version, UUID studentId, UUID subjectId, String title, Instant dueAt,
                                            List<ContentBlock> blocks, AssignmentStatus status) {
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setVersion(version);
        request.setStudentId(studentId);
        request.setSubjectId(subjectId);
        request.setTitle(title);
        request.setAssignmentType(AssignmentType.FREEFORM);
        request.setContentBlocks(blocks);
        request.setQuizSchema(null);
        request.setDueAt(dueAt);
        request.setStatus(status);
        return request;
    }

    private List<ContentBlock> blocks(String content, List<MultipartFile> files, UUID ownerId, List<UUID> existing) {
        List<ContentBlock> result = new ArrayList<>();
        if (content != null && !content.isBlank()) result.add(new ContentBlock("TEXT", content.trim(), null, null, null));
        existing.forEach(id -> result.add(new ContentBlock("FILE", null, id, null, null)));
        if (files != null) for (MultipartFile file : files) if (file != null && !file.isEmpty()) {
            AttachmentView uploaded = attachmentService.uploadAttachment(ownerId, AttachableType.ASSIGNMENT, file);
            result.add(new ContentBlock("FILE", null, uploaded.getId(), null, uploaded.getOriginalFilename()));
        }
        return result;
    }

    private Instant instant(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value).atZone(VIETNAM_ZONE).toInstant();
    }

    @SuppressWarnings("unchecked")
    private List<UUID> attachmentIds(Map<String, Object> assignment) {
        return contentBlocks(assignment).stream()
                .map(ContentBlock::getAttachmentId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private List<ContentBlock> contentBlocks(Map<String, Object> assignment) {
        Object raw = assignment.get("contentBlocks");
        if (raw == null) return List.of();
        try {
            return objectMapper.convertValue(raw, new TypeReference<List<ContentBlock>>() {});
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private List<AttachmentView> attachments(Map<String, Object> assignment) {
        return attachmentIds(assignment).stream().map(attachmentFacade::getAttachmentView).toList();
    }

    private List<AttachmentView> attachmentViews(List<ContentBlock> blocks) {
        if (blocks == null) return List.of();
        return blocks.stream().map(ContentBlock::getAttachmentId).filter(java.util.Objects::nonNull).distinct()
                .map(attachmentFacade::getAttachmentView).toList();
    }

    public String safeAttachmentUrl(AttachmentView file) {
        if (file == null || file.getSecureUrl() == null) return null;
        String url = file.getSecureUrl().trim();
        return url.startsWith("https://") || url.startsWith("http://") ? url : null;
    }

    @SuppressWarnings("unchecked")
    private AssignmentListRow listRow(Map<String, Object> data) {
        List<?> submissions = (List<?>) data.getOrDefault("submissions", List.of());
        Object submission = submissions.isEmpty() ? null : submissions.get(0);
        return new AssignmentListRow(data, dueLabel(data.get("dueAt")), statusLabel(data.get("status")),
                String.valueOf(data.getOrDefault("studentName", "Học viên")),
                String.valueOf(data.getOrDefault("subjectName", "Môn học")), submissionLabel(submission));
    }

    private SubmissionRow submissionRow(Map<String, Object> assignment) {
        Object raw = assignment.get("submissions");
        if (!(raw instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof SubmissionDetail data)) return null;
        return new SubmissionRow(data.getId().toString(), submissionLabel(data), data.getStatus() == SubmissionStatus.SUBMITTED);
    }

    private String submissionLabel(Object value) {
        Object status = value instanceof SubmissionDetail detail ? detail.getStatus() : null;
        if (value instanceof Map<?, ?> data) status = data.get("status");
        if (status == null) return "Chưa nộp";
        return switch (String.valueOf(status)) {
            case "SUBMITTED" -> "Chờ chấm";
            case "GRADED" -> "Đã chấm";
            default -> "Bản nháp";
        };
    }

    private String dueLabel(Object value) {
        if (!(value instanceof Instant instant)) return "Chưa đặt hạn";
        return DATE_TIME.format(instant);
    }

    private String statusLabel(Object value) {
        return switch (String.valueOf(value)) {
            case "DRAFT" -> "Bản nháp";
            case "PUBLISHED" -> "Đã giao";
            case "CLOSED" -> "Đã đóng";
            default -> "Không rõ trạng thái";
        };
    }

    @SuppressWarnings("unchecked")
    private AssignmentForm formFrom(Map<String, Object> assignment) {
        String content = "";
        Object raw = assignment.get("contentBlocks");
        if (raw != null) content = contentBlocks(assignment).stream()
                .filter(block -> "TEXT".equals(block.getType())).map(ContentBlock::getContent).filter(java.util.Objects::nonNull).findFirst().orElse("");
        Object dueAt = assignment.get("dueAt");
        String localDue = dueAt instanceof Instant instant ? LocalDateTime.ofInstant(instant, VIETNAM_ZONE).toString() : "";
        return new AssignmentForm((UUID) assignment.get("studentId"), (UUID) assignment.get("subjectId"),
                String.valueOf(assignment.getOrDefault("title", "")), localDue, content,
                ((Number) assignment.getOrDefault("version", 0L)).longValue());
    }

    private String publicMessage(RuntimeException ex) {
        if (ex instanceof BusinessException business) return switch (business.getErrorCode()) {
            case CONCURRENT_MODIFICATION -> "Dữ liệu đã thay đổi. Tải lại trang rồi thử lại.";
            case FILE_TOO_LARGE -> "Tệp vượt quá dung lượng cho phép (20 MB mỗi tệp).";
            case FILE_TYPE_NOT_ALLOWED -> "Định dạng tệp này chưa được hỗ trợ.";
            case FILE_UPLOAD_FAILED -> "Chưa tải được tệp. Hãy thử lại.";
            case ASSIGNMENT_DUE_DATE_PASSED -> "Hạn nộp phải ở trong tương lai.";
            case ASSIGNMENT_INVALID_STATE -> "Trạng thái bài tập không cho phép thao tác này.";
            case SUBMISSION_NOT_SUBMITTED -> "Chỉ có thể chấm bài đã nộp.";
            case SUBMISSION_ALREADY_GRADED -> "Bài này đã được chấm.";
            case LEARNING_RELATIONSHIP_NOT_FOUND -> "Chỉ có thể giao bài cho học viên đang học với bạn.";
            case VALIDATION_ERROR -> business.getMessage() == null ? "Kiểm tra lại các trường đã nhập." : business.getMessage();
            default -> "Chưa thực hiện được thao tác. Tải lại trang rồi thử lại.";
        };
        return "Chưa thực hiện được thao tác. Kiểm tra dữ liệu rồi thử lại.";
    }

    public record AssignmentForm(UUID studentId, UUID subjectId, String title, String dueAt, String content, long version) {}
    public record StudentOption(UUID id, String name) {}
    public record SubjectOption(UUID id, String name) {}
    public record AssignmentListRow(Map<String, Object> data, String dueLabel, String statusLabel, String studentName, String subjectName, String submissionLabel) {}
    public record SubmissionRow(String id, String label, boolean gradeable) {}
}
