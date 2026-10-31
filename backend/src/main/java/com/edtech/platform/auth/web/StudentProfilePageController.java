package com.edtech.platform.auth.web;

import com.edtech.platform.auth.dto.request.UpdateParentContactRequest;
import com.edtech.platform.auth.dto.response.ParentContactResponse;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.auth.service.ParentContactService;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class StudentProfilePageController {
    private static final Logger log = LoggerFactory.getLogger(StudentProfilePageController.class);

    private final ParentContactService parentContacts;
    private final IdentityFacade identities;

    @InitBinder("profileForm")
    void trimFormValues(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/student/profile")
    public String page(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        requireStudent(user);
        addPageContext(user, model);
        try {
            model.addAttribute("profileForm", StudentProfileForm.from(parentContacts.get(user.id())));
            model.addAttribute("contactUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Student profile contact could not be loaded");
            model.addAttribute("profileForm", new StudentProfileForm());
            model.addAttribute("contactUnavailable", true);
        }
        return "student/profile";
    }

    @PostMapping("/student/profile")
    public String update(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("profileForm") StudentProfileForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirect) {
        requireStudent(user);
        if (bindingResult.hasErrors()) {
            addPageContext(user, model);
            model.addAttribute("contactUnavailable", false);
            model.addAttribute("formError", "Kiểm tra lại thông tin được đánh dấu bên dưới.");
            return "student/profile";
        }

        try {
            parentContacts.update(user.id(), new UpdateParentContactRequest(
                    form.getParentFullName(), form.getParentPhone(), form.getParentEmail(), form.getNotifyParent()));
            redirect.addFlashAttribute("profileNotice", "Đã lưu thông tin liên hệ phụ huynh.");
            return "redirect:/student/profile";
        } catch (RuntimeException ex) {
            log.warn("Student profile contact could not be updated");
            addPageContext(user, model);
            model.addAttribute("contactUnavailable", false);
            model.addAttribute("formError", "Chưa lưu được thông tin. Vui lòng thử lại.");
            return "student/profile";
        }
    }

    private void addPageContext(AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "profile");
        model.addAttribute("accountEmail", user.email());
        try {
            Optional<IdentitySnapshot> identity = identities.getIdentity(user.id());
            model.addAttribute("identity", identity.orElse(null));
            model.addAttribute("accountStatusLabel", identity.map(value -> statusLabel(value.statusName())).orElse(null));
        } catch (RuntimeException ex) {
            log.warn("Student profile identity could not be loaded");
            model.addAttribute("identity", null);
            model.addAttribute("accountStatusLabel", null);
        }
    }

    private void requireStudent(AuthenticatedUser user) {
        if (user == null || !"STUDENT".equals(user.role())) {
            throw new AccessDeniedException("Student workspace only");
        }
    }

    private String statusLabel(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> "Đang hoạt động";
            case "PENDING_VERIFICATION" -> "Chờ xác minh email";
            case "LOCKED" -> "Tạm khóa";
            case "DISABLED" -> "Đã vô hiệu hóa";
            default -> null;
        };
    }

    public static class StudentProfileForm {
        @Pattern(regexp = "^$|^.{3,}$", message = "Nhập ít nhất 3 ký tự hoặc để trống.")
        private String parentFullName = "";

        @Pattern(regexp = "^$|^[0-9]{10,11}$", message = "Nhập số điện thoại gồm 10 hoặc 11 chữ số.")
        private String parentPhone = "";

        @Email(message = "Địa chỉ email chưa đúng định dạng.")
        private String parentEmail = "";

        private Boolean notifyParent = false;

        static StudentProfileForm from(ParentContactResponse response) {
            StudentProfileForm form = new StudentProfileForm();
            form.setParentFullName(valueOrEmpty(response.parentFullName()));
            form.setParentPhone(valueOrEmpty(response.parentPhone()));
            form.setParentEmail(valueOrEmpty(response.parentEmail()));
            form.setNotifyParent(Boolean.TRUE.equals(response.notifyParent()));
            return form;
        }

        private static String valueOrEmpty(String value) {
            return value == null ? "" : value;
        }

        public String getParentFullName() { return parentFullName; }
        public void setParentFullName(String parentFullName) { this.parentFullName = parentFullName; }
        public String getParentPhone() { return parentPhone; }
        public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
        public String getParentEmail() { return parentEmail; }
        public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }
        public Boolean getNotifyParent() { return notifyParent; }
        public void setNotifyParent(Boolean notifyParent) { this.notifyParent = notifyParent; }
    }
}
