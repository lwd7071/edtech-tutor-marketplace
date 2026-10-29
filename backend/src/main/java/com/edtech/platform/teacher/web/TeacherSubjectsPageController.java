package com.edtech.platform.teacher.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherSubjectsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherSubjectsPageController.class);
    private final TeacherSubjectService teacherSubjects;
    private final SubjectFacade subjects;

    @GetMapping("/teacher/subjects")
    public String page(@AuthenticationPrincipal AuthenticatedUser user, Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "subjects");
        if (!model.containsAttribute("assignForm")) model.addAttribute("assignForm", new AssignForm());
        loadPage(user, model);
        return "workspace/teacher-subjects";
    }

    @PostMapping("/teacher/subjects/assign")
    public String assign(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("assignForm") AssignForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeWorkspaceNav", "subjects");
            model.addAttribute("subjectFormError", "Chọn một môn học trước khi thêm.");
            loadPage(user, model);
            return "workspace/teacher-subjects";
        }
        try {
            teacherSubjects.assignSubject(user.id(), form.getSubjectId(), new AssignSubjectRequest(
                    clean(form.getLevelDescription()), clean(form.getExperienceDescription())));
            redirect.addFlashAttribute("subjectNotice", "Đã thêm môn dạy vào hồ sơ.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("subjectError", errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher subject assignment failed");
            redirect.addFlashAttribute("subjectError", "Chưa thêm được môn dạy. Tải lại trang rồi thử lại.");
        }
        return "redirect:/teacher/subjects";
    }

    @PostMapping("/teacher/subjects/{subjectId}/remove")
    public String remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID subjectId,
                         RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        try {
            teacherSubjects.unassignSubject(user.id(), subjectId);
            redirect.addFlashAttribute("subjectNotice", "Đã gỡ môn dạy khỏi hồ sơ.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("subjectError", errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher subject removal failed");
            redirect.addFlashAttribute("subjectError", "Chưa gỡ được môn dạy. Tải lại trang rồi thử lại.");
        }
        return "redirect:/teacher/subjects";
    }

    private void loadPage(AuthenticatedUser user, Model model) {
        List<TeacherSubjectView> assigned;
        try {
            assigned = teacherSubjects.getSubjects(user.id());
            model.addAttribute("teacherSubjects", assigned.stream().map(item -> new TeacherSubjectRow(
                    item.subject().id(), item.subject().name(), educationLevelLabel(item.subject().educationLevel()),
                    item.levelDescription(), item.experienceDescription(), item.isActive())).toList());
            model.addAttribute("subjectsUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher subjects could not be loaded");
            assigned = List.of();
            model.addAttribute("teacherSubjects", List.of());
            model.addAttribute("subjectsUnavailable", true);
        }
        try {
            List<SubjectSnapshot> active = subjects.getActiveSubjects();
            var assignedIds = assigned.stream().map(item -> item.subject().id()).collect(java.util.stream.Collectors.toSet());
            model.addAttribute("availableSubjects", active.stream().filter(item -> !assignedIds.contains(item.id()))
                    .map(item -> new SubjectOption(item.id(), item.name(), educationLevelLabel(item.educationLevel()))).toList());
            model.addAttribute("catalogUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Active subject catalog could not be loaded");
            model.addAttribute("availableSubjects", List.of());
            model.addAttribute("catalogUnavailable", true);
        }
    }

    private String errorMessage(ErrorCode code) {
        if (code == ErrorCode.SUBJECT_ALREADY_ASSIGNED) return "Môn học này đã có trong hồ sơ.";
        if (code == ErrorCode.SUBJECT_NOT_ASSIGNED) return "Môn học không còn trong hồ sơ. Danh sách đã được tải lại.";
        if (code == ErrorCode.TEACHER_PROFILE_NOT_FOUND) return "Chưa tìm thấy hồ sơ gia sư của bạn.";
        if (code == ErrorCode.RESOURCE_NOT_FOUND) return "Môn học không còn hoạt động. Hãy chọn lại từ danh sách.";
        return "Thao tác chưa thực hiện được. Tải lại trang rồi thử lại.";
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String educationLevelLabel(String educationLevel) {
        if (educationLevel == null) return "Cấp học đang cập nhật";
        return switch (educationLevel) {
            case "ELEMENTARY", "PRIMARY_SCHOOL" -> "Tiểu học";
            case "MIDDLE_SCHOOL" -> "Trung học cơ sở";
            case "HIGH_SCHOOL" -> "Trung học phổ thông";
            case "UNIVERSITY" -> "Đại học";
            case "OTHER" -> "Khác";
            default -> "Cấp học đang cập nhật";
        };
    }

    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public static class AssignForm {
        @NotNull(message = "Chọn một môn học.")
        private UUID subjectId;
        private String levelDescription;
        private String experienceDescription;
        public UUID getSubjectId() { return subjectId; }
        public void setSubjectId(UUID value) { subjectId = value; }
        public String getLevelDescription() { return levelDescription; }
        public void setLevelDescription(String value) { levelDescription = value; }
        public String getExperienceDescription() { return experienceDescription; }
        public void setExperienceDescription(String value) { experienceDescription = value; }
    }

    public record TeacherSubjectRow(UUID subjectId, String name, String educationLevelLabel,
                                    String levelDescription, String experienceDescription, boolean active) {}
    public record SubjectOption(UUID id, String name, String educationLevelLabel) {}
}
