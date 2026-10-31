package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.dto.ChangePackageStatusRequest;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.UpsertPricingPackageRequest;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherPackagesPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherPackagesPageController.class);
    private static final int PAGE_SIZE = 10;

    private final PricingPackageService packages;
    private final TeacherSubjectService subjects;
    private final EnrollmentFacade enrollments;
    private final TeacherFacade teachers;

    @GetMapping("/teacher/packages")
    public String list(@AuthenticationPrincipal AuthenticatedUser user, Model model,
                       HttpServletResponse response, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "packages");
        try {
            int safePage = Math.max(0, page);
            var packagePage = packages.getOwnPackages(user.id(), PageRequest.of(safePage, PAGE_SIZE,
                    Sort.by(Sort.Direction.DESC, "createdAt")));
            if (safePage > 0 && packagePage.getTotalPages() > 0 && safePage >= packagePage.getTotalPages()) {
                return "redirect:/teacher/packages?page=" + (packagePage.getTotalPages() - 1);
            }
            model.addAttribute("packagePage", packagePage);
            model.addAttribute("packagesUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher packages could not be loaded");
            model.addAttribute("packagePage", org.springframework.data.domain.Page.empty());
            model.addAttribute("packagesUnavailable", true);
        }
        return "workspace/teacher-packages";
    }

    @GetMapping("/teacher/packages/create")
    public String createPage(@AuthenticationPrincipal AuthenticatedUser user, Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "packages");
        if (!model.containsAttribute("packageForm")) model.addAttribute("packageForm", new PackageForm());
        model.addAttribute("pageTitle", "Tạo gói học");
        model.addAttribute("formAction", "/teacher/packages/create");
        model.addAttribute("isCreate", true);
        loadApprovalState(user, model);
        loadAssignedSubjects(user, model);
        return "workspace/teacher-package-form";
    }

    @PostMapping("/teacher/packages/create")
    public String create(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("packageForm") PackageForm form,
                         BindingResult binding, Model model, RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (binding.hasErrors()) {
            model.addAttribute("packageFormError", "Có thông tin chưa hợp lệ. Kiểm tra các trường được đánh dấu.");
            prepareCreateForm(user, model);
            return "workspace/teacher-package-form";
        }
        try {
            PricingPackageView created = packages.createPackage(user.id(), form.toRequest(0L));
            redirect.addFlashAttribute("packageNotice", "Đã tạo gói học.");
            return "redirect:/teacher/packages/" + created.id();
        } catch (BusinessException ex) {
            model.addAttribute("packageFormError", errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher package creation failed");
            model.addAttribute("packageFormError", "Chưa tạo được gói học. Kiểm tra kết nối rồi thử lại.");
        }
        prepareCreateForm(user, model);
        return "workspace/teacher-package-form";
    }

    @GetMapping("/teacher/packages/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id, Model model,
                         HttpServletResponse response, RedirectAttributes redirect) {
        requireTeacher(user);
        noStore(response);
        try {
            loadDetail(user, id, model);
            if (!model.containsAttribute("packageForm")) model.addAttribute("packageForm", PackageForm.from((PricingPackageView) model.getAttribute("pricingPackage")));
            return "workspace/teacher-package-detail";
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("packageError", errorMessage(ex.getErrorCode()));
            return "redirect:/teacher/packages";
        }
    }

    @PostMapping("/teacher/packages/{id}/update")
    public String update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @Valid @ModelAttribute("packageForm") PackageForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (!binding.hasErrors()) {
            try {
                packages.updatePackage(user.id(), id, form.toRequest(form.getVersion()));
                redirect.addFlashAttribute("packageNotice", "Đã cập nhật gói học.");
                return "redirect:/teacher/packages/" + id;
            } catch (BusinessException ex) {
                model.addAttribute("packageFormError", errorMessage(ex.getErrorCode()));
            } catch (RuntimeException ex) {
                log.warn("Teacher package update failed");
                model.addAttribute("packageFormError", "Chưa cập nhật được gói học. Tải lại trang rồi thử lại.");
            }
        }
        if (binding.hasErrors()) model.addAttribute("packageFormError", "Có thông tin chưa hợp lệ. Kiểm tra các trường được đánh dấu.");
        try {
            loadDetail(user, id, model);
            PricingPackageView current = (PricingPackageView) model.getAttribute("pricingPackage");
            if (current != null && current.version() != form.getVersion()) form.setVersion(current.version());
            model.addAttribute("packageForm", form);
            model.addAttribute("pageTitle", current == null ? "Chi tiết gói học" : current.name());
            model.addAttribute("formAction", "/teacher/packages/" + id + "/update");
            return "workspace/teacher-package-detail";
        } catch (BusinessException ex) {
            return "redirect:/teacher/packages";
        }
    }

    @PostMapping("/teacher/packages/{id}/status")
    public String changeStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                               @Valid @ModelAttribute("statusForm") StatusForm form,
                               BindingResult binding, RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("packageError", "Trạng thái gói không hợp lệ. Tải lại trang rồi thử lại.");
            return "redirect:/teacher/packages/" + id;
        }
        try {
            packages.changeStatus(user.id(), id, new ChangePackageStatusRequest(form.getStatus(), form.getVersion()));
            redirect.addFlashAttribute("packageNotice", "Đã cập nhật trạng thái gói.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("packageError", errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher package status update failed");
            redirect.addFlashAttribute("packageError", "Chưa cập nhật được trạng thái. Tải lại trang rồi thử lại.");
        }
        return "redirect:/teacher/packages/" + id;
    }

    private void prepareCreateForm(AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "packages");
        model.addAttribute("pageTitle", "Tạo gói học");
        model.addAttribute("formAction", "/teacher/packages/create");
        model.addAttribute("isCreate", true);
        loadApprovalState(user, model);
        loadAssignedSubjects(user, model);
    }

    private void loadDetail(AuthenticatedUser user, UUID id, Model model) {
        PricingPackageView pkg = packages.getOwnPackage(user.id(), id);
        model.addAttribute("activeWorkspaceNav", "packages");
        model.addAttribute("pricingPackage", pkg);
        model.addAttribute("pageTitle", pkg.name());
        model.addAttribute("formAction", "/teacher/packages/" + id + "/update");
        model.addAttribute("isCreate", false);
        loadApprovalState(user, model);
        loadAssignedSubjects(user, model);
        try {
            model.addAttribute("packageSold", enrollments.hasStudentPackage(id));
            model.addAttribute("purchaseCheckUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher package purchase state could not be checked");
            model.addAttribute("packageSold", true);
            model.addAttribute("purchaseCheckUnavailable", true);
        }
    }

    private void loadAssignedSubjects(AuthenticatedUser user, Model model) {
        try {
            List<TeacherSubjectView> assigned = subjects.getSubjects(user.id());
            model.addAttribute("assignedSubjects", assigned.stream().filter(TeacherSubjectView::isActive)
                    .map(item -> new SubjectOption(item.subject().id(), item.subject().name())).toList());
            model.addAttribute("subjectsUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher package subject options could not be loaded");
            model.addAttribute("assignedSubjects", List.of());
            model.addAttribute("subjectsUnavailable", true);
        }
    }

    private void loadApprovalState(AuthenticatedUser user, Model model) {
        try {
            var teacher = teachers.getTeacherByUserId(user.id());
            model.addAttribute("teacherApproved", teacher != null && "APPROVED".equalsIgnoreCase(teacher.status()));
            model.addAttribute("approvalCheckUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher approval state could not be checked");
            model.addAttribute("teacherApproved", false);
            model.addAttribute("approvalCheckUnavailable", true);
        }
    }

    private String errorMessage(ErrorCode code) {
        return switch (code) {
            case TEACHER_NOT_APPROVED -> "Hồ sơ gia sư cần được duyệt trước khi tạo hoặc bật gói.";
            case TEACHER_PROFILE_NOT_FOUND -> "Chưa tìm thấy hồ sơ gia sư của bạn.";
            case SUBJECT_NOT_ASSIGNED -> "Môn học không còn được gắn với hồ sơ. Hãy tải lại và chọn môn khác.";
            case PRICING_PACKAGE_NOT_FOUND, RESOURCE_NOT_FOUND -> "Không tìm thấy gói học này trong hồ sơ của bạn.";
            case FORBIDDEN_RESOURCE -> "Bạn không có quyền thay đổi gói học này.";
            case PACKAGE_IMMUTABLE_AFTER_PURCHASE -> "Gói đã có học viên mua nên không thể sửa nội dung.";
            case PACKAGE_INVALID_STATE -> "Gói đã có học viên mua; chỉ có thể chuyển sang tạm ngưng.";
            case CONCURRENT_MODIFICATION -> "Gói vừa được thay đổi. Kiểm tra lại thông tin trước khi lưu.";
            case VALIDATION_ERROR -> "Thông tin gói chưa hợp lệ. Kiểm tra lại các trường.";
            default -> "Thao tác chưa thực hiện được. Tải lại trang rồi thử lại.";
        };
    }

    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record SubjectOption(UUID id, String name) {}

    public static class PackageForm {
        @NotNull(message = "Chọn môn học.") private UUID subjectId;
        @NotBlank(message = "Nhập tên gói.") @Size(max = 150, message = "Tên gói tối đa 150 ký tự.") private String name;
        private String description;
        @NotNull(message = "Nhập tổng số buổi.") @Min(value = 1, message = "Số buổi phải lớn hơn 0.") private Integer totalSessions;
        @NotNull(message = "Nhập thời hạn gói.") @Min(value = 1, message = "Thời hạn phải lớn hơn 0.") private Integer durationDays;
        @NotNull(message = "Nhập giá gói.") @Min(value = 1, message = "Giá gói phải lớn hơn 0.") private Long priceVnd;
        @NotNull(message = "Nhập thời lượng mỗi buổi.") @Min(value = 1, message = "Thời lượng phải lớn hơn 0.") private Integer sessionDurationMinutes;
        @NotNull(message = "Chọn trạng thái.") private PackageStatus status = PackageStatus.DRAFT;
        @NotNull @Min(0) private Long version = 0L;
        public static PackageForm from(PricingPackageView view) {
            PackageForm form = new PackageForm();
            form.subjectId = view.subjectId(); form.name = view.name(); form.description = view.description();
            form.totalSessions = view.totalSessions(); form.durationDays = view.durationDays(); form.priceVnd = view.priceVnd();
            form.sessionDurationMinutes = view.sessionDurationMinutes(); form.status = view.status(); form.version = view.version();
            return form;
        }
        UpsertPricingPackageRequest toRequest(Long requestVersion) {
            return new UpsertPricingPackageRequest(subjectId, name == null ? null : name.trim(),
                    description == null || description.isBlank() ? null : description.trim(), totalSessions,
                    durationDays, priceVnd, sessionDurationMinutes, status, requestVersion);
        }
        public UUID getSubjectId() { return subjectId; } public void setSubjectId(UUID value) { subjectId = value; }
        public String getName() { return name; } public void setName(String value) { name = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public Integer getTotalSessions() { return totalSessions; } public void setTotalSessions(Integer value) { totalSessions = value; }
        public Integer getDurationDays() { return durationDays; } public void setDurationDays(Integer value) { durationDays = value; }
        public Long getPriceVnd() { return priceVnd; } public void setPriceVnd(Long value) { priceVnd = value; }
        public Integer getSessionDurationMinutes() { return sessionDurationMinutes; } public void setSessionDurationMinutes(Integer value) { sessionDurationMinutes = value; }
        public PackageStatus getStatus() { return status; } public void setStatus(PackageStatus value) { status = value; }
        public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
    }

    public static class StatusForm {
        @NotNull private PackageStatus status;
        @NotNull @Min(0) private Long version;
        public PackageStatus getStatus() { return status; } public void setStatus(PackageStatus value) { status = value; }
        public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
    }
}
