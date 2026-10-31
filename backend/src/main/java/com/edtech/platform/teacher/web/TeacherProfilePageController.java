package com.edtech.platform.teacher.web;

import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.AdministrativeWard;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.dto.UpdateTeacherProfileRequest;
import com.edtech.platform.teacher.dto.UpdateTeacherResidenceRequest;
import com.edtech.platform.teacher.service.TeacherProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

@Controller
@RequiredArgsConstructor
public class TeacherProfilePageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherProfilePageController.class);
    private final TeacherProfileService profiles;
    private final AdministrativeLocationService locations;

    @GetMapping("/teacher/profile")
    public String page(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) String provinceCode,
                       Model model) {
        requireTeacher(user);
        try {
            TeacherProfileDetail profile = profiles.getProfile(user.id());
            String selectedProvince = provinceCode != null ? provinceCode : profile.provinceCode();
            addPageContext(model, profile, selectedProvince, profileForm(profile),
                    new ResidenceForm(selectedProvince, java.util.Objects.equals(selectedProvince, profile.provinceCode()) ? profile.wardCode() : null));
            model.addAttribute("profileUnavailable", false);
        model.addAttribute("formHasErrors", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher profile could not be loaded");
            model.addAttribute("activeWorkspaceNav", "profile");
            model.addAttribute("profileUnavailable", true);
            model.addAttribute("profile", null);
        }
        return "workspace/teacher-profile";
    }

    @PostMapping("/teacher/profile/update")
    public String update(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute("profileForm") ProfileForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        requireTeacher(user);
        List<String> languages = form.languages();
        if (languages.size() > 10) bindingResult.rejectValue("languagesCsv", "size", "Tối đa 10 ngôn ngữ.");
        if (languages.stream().anyMatch(value -> value.length() > 50)) bindingResult.rejectValue("languagesCsv", "size", "Mỗi ngôn ngữ tối đa 50 ký tự.");
        if (!validOptionalHttpUrl(form.getIntroductionVideoUrl())) bindingResult.rejectValue("introductionVideoUrl", "url", "Nhập link video HTTP hoặc HTTPS hợp lệ.");
        if (bindingResult.hasErrors()) {
            renderAfterMutation(user, model, form, new ResidenceForm(), null);
            model.addAttribute("formHasErrors", true);
            return "workspace/teacher-profile";
        }
        try {
            profiles.updateProfile(user.id(), new UpdateTeacherProfileRequest(form.getBio(), form.getYearsOfExperience(),
                    languages, form.getSupportsOnline(), form.getSupportsOffline(), blankToNull(form.getIntroductionVideoUrl())));
            redirect.addFlashAttribute("profileNotice", "Đã lưu thông tin giảng dạy.");
            return "redirect:/teacher/profile";
        } catch (RuntimeException ex) {
            log.warn("Teacher profile could not be updated");
            renderAfterMutation(user, model, form, new ResidenceForm(), "Chưa lưu được hồ sơ. Kiểm tra trạng thái và thử lại.");
            return "workspace/teacher-profile";
        }
    }

    @PostMapping("/teacher/profile/residence")
    public String updateResidence(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @ModelAttribute("residenceForm") ResidenceForm form,
                                  BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        requireTeacher(user);
        if (!bindingResult.hasErrors() && form.getWardCode() != null && !form.getWardCode().isBlank()) {
            try {
                boolean belongs = locations.getWards(form.getProvinceCode()).stream()
                        .anyMatch(ward -> ward.code().equals(form.getWardCode()));
                if (!belongs) bindingResult.rejectValue("wardCode", "invalid", "Xã/phường không thuộc tỉnh/thành đã chọn.");
            } catch (RuntimeException ex) {
                bindingResult.rejectValue("provinceCode", "unavailable", "Chưa tải được danh sách địa phương. Thử lại.");
            }
        }
        if (bindingResult.hasErrors()) {
            renderAfterMutation(user, model, new ProfileForm(), form, "Kiểm tra lại nơi ở được đánh dấu bên dưới.");
            model.addAttribute("formHasErrors", true);
            return "workspace/teacher-profile";
        }
        try {
            profiles.updateResidence(user.id(), new UpdateTeacherResidenceRequest(blankToNull(form.getProvinceCode()), blankToNull(form.getWardCode())));
            redirect.addFlashAttribute("profileNotice", "Đã cập nhật khu vực nơi ở.");
            return "redirect:/teacher/profile";
        } catch (RuntimeException ex) {
            log.warn("Teacher residence could not be updated");
            renderAfterMutation(user, model, new ProfileForm(), form, "Chưa cập nhật được nơi ở. Vui lòng thử lại.");
            return "workspace/teacher-profile";
        }
    }

    @PostMapping("/teacher/profile/submit")
    public String submit(@AuthenticationPrincipal AuthenticatedUser user, RedirectAttributes redirect) {
        requireTeacher(user);
        try {
            profiles.submitProfile(user.id());
            redirect.addFlashAttribute("profileNotice", "Đã gửi hồ sơ để xét duyệt.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("profileError", ex.getErrorCode() == ErrorCode.TEACHER_PROFILE_INVALID_STATE
                    ? "Hồ sơ đang chờ duyệt hoặc chưa thể gửi lại." : "Chưa gửi được hồ sơ. Tải lại trang và thử lại.");
        } catch (RuntimeException ex) {
            log.warn("Teacher profile could not be submitted");
            redirect.addFlashAttribute("profileError", "Chưa gửi được hồ sơ. Tải lại trang và thử lại.");
        }
        return "redirect:/teacher/profile";
    }

    private void renderAfterMutation(AuthenticatedUser user, Model model, ProfileForm profileForm,
                                     ResidenceForm residenceForm, String formError) {
        try {
            TeacherProfileDetail profile = profiles.getProfile(user.id());
            boolean profileFormSubmitted = model.containsAttribute("org.springframework.validation.BindingResult.profileForm");
            boolean residenceFormSubmitted = model.containsAttribute("org.springframework.validation.BindingResult.residenceForm");
            String selectedProvince = residenceFormSubmitted ? residenceForm.getProvinceCode() : profile.provinceCode();
            addPageContext(model, profile, selectedProvince,
                    profileFormSubmitted ? profileForm : profileForm(profile),
                    residenceFormSubmitted ? residenceForm : new ResidenceForm(profile.provinceCode(), profile.wardCode()));
            model.addAttribute("profileUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher profile context could not be reloaded after form error");
            model.addAttribute("activeWorkspaceNav", "profile");
            model.addAttribute("profileUnavailable", true);
        }
        if (formError != null) model.addAttribute("formError", formError);
    }

    private void addPageContext(Model model, TeacherProfileDetail profile, String selectedProvince,
                                ProfileForm profileForm, ResidenceForm residenceForm) {
        model.addAttribute("activeWorkspaceNav", "profile");
        model.addAttribute("profile", profile);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("residenceForm", residenceForm);
        model.addAttribute("profileStatusLabel", statusLabel(profile.profileStatus()));
        model.addAttribute("canSubmit", profile.profileStatus() == ProfileStatus.DRAFT || profile.profileStatus() == ProfileStatus.REJECTED);
        model.addAttribute("canEdit", profile.profileStatus() != ProfileStatus.PENDING_APPROVAL);
        model.addAttribute("selectedProvince", selectedProvince);
        try {
            List<AdministrativeProvince> provinces = locations.getProvinces();
            model.addAttribute("provinces", provinces);
            model.addAttribute("selectedProvinceLabel", provinces.stream()
                    .filter(province -> province.code().equals(selectedProvince)).map(AdministrativeProvince::name).findFirst().orElse("Chưa chọn"));
            model.addAttribute("wards", selectedProvince != null && selectedProvince.matches("^[0-9]{2}$")
                    ? locations.getWards(selectedProvince) : List.<AdministrativeWard>of());
            model.addAttribute("locationsUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher profile locations could not be loaded");
            model.addAttribute("provinces", List.<AdministrativeProvince>of());
            model.addAttribute("selectedProvinceLabel", "Chưa tải được danh sách");
            model.addAttribute("wards", List.<AdministrativeWard>of());
            model.addAttribute("locationsUnavailable", true);
        }
    }

    private ProfileForm profileForm(TeacherProfileDetail profile) {
        return new ProfileForm(profile.bio(), profile.yearsOfExperience(),
                profile.languages() == null ? "" : String.join(", ", profile.languages()),
                profile.supportsOnline(), profile.supportsOffline(), profile.introductionVideoUrl());
    }

    private String statusLabel(ProfileStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING_APPROVAL -> "Đang chờ duyệt";
            case APPROVED -> "Đã được duyệt";
            case REJECTED -> "Cần bổ sung";
        };
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private boolean validOptionalHttpUrl(String value) {
        if (value == null || value.isBlank()) return true;
        if (!value.equals(value.trim())) return false;
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException ex) {
            return false;
        }
    }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public static class ProfileForm {
        @Pattern(regexp = "^[^<]*$", message = "Phần giới thiệu không được chứa thẻ HTML.")
        @Size(max = 5000, message = "Phần giới thiệu tối đa 5.000 ký tự.")
        private String bio = "";
        @Min(value = 0, message = "Số năm kinh nghiệm không được nhỏ hơn 0.")
        @Max(value = 80, message = "Số năm kinh nghiệm tối đa là 80.")
        private Integer yearsOfExperience;
        @Size(max = 509, message = "Danh sách ngôn ngữ quá dài.")
        private String languagesCsv = "";
        @NotNull(message = "Chọn hình thức dạy online.") private Boolean supportsOnline = false;
        @NotNull(message = "Chọn hình thức dạy trực tiếp.") private Boolean supportsOffline = false;
        @Size(max = 500, message = "Đường dẫn video tối đa 500 ký tự.")
        private String introductionVideoUrl = "";
        public ProfileForm() {}
        public ProfileForm(String bio, Integer years, String languages, Boolean online, Boolean offline, String video) {
            this.bio = bio == null ? "" : bio; this.yearsOfExperience = years; this.languagesCsv = languages == null ? "" : languages;
            this.supportsOnline = online; this.supportsOffline = offline; this.introductionVideoUrl = video == null ? "" : video;
        }
        public List<String> languages() { return languagesCsv == null || languagesCsv.isBlank() ? List.of() : java.util.Arrays.stream(languagesCsv.split(",", -1)).map(String::trim).filter(value -> !value.isBlank()).toList(); }
        public String getBio() { return bio; } public void setBio(String value) { bio = value; }
        public Integer getYearsOfExperience() { return yearsOfExperience; } public void setYearsOfExperience(Integer value) { yearsOfExperience = value; }
        public String getLanguagesCsv() { return languagesCsv; } public void setLanguagesCsv(String value) { languagesCsv = value; }
        public Boolean getSupportsOnline() { return supportsOnline; } public void setSupportsOnline(Boolean value) { supportsOnline = value; }
        public Boolean getSupportsOffline() { return supportsOffline; } public void setSupportsOffline(Boolean value) { supportsOffline = value; }
        public String getIntroductionVideoUrl() { return introductionVideoUrl; } public void setIntroductionVideoUrl(String value) { introductionVideoUrl = value; }
    }

    public static class ResidenceForm {
        @Pattern(regexp = "^$|^[0-9]{2}$", message = "Chọn tỉnh/thành hợp lệ.") private String provinceCode = "";
        @Pattern(regexp = "^$|^[0-9]{5}$", message = "Chọn xã/phường hợp lệ.") private String wardCode = "";
        @jakarta.validation.constraints.AssertTrue(message = "Cần chọn tỉnh/thành trước khi chọn xã/phường.")
        public boolean isProvincePresentWhenWardSelected() { return wardCode == null || wardCode.isBlank() || provinceCode != null && !provinceCode.isBlank(); }
        public ResidenceForm() {}
        public ResidenceForm(String provinceCode, String wardCode) { this.provinceCode = provinceCode == null ? "" : provinceCode; this.wardCode = wardCode == null ? "" : wardCode; }
        public String getProvinceCode() { return provinceCode; } public void setProvinceCode(String value) { provinceCode = value; }
        public String getWardCode() { return wardCode; } public void setWardCode(String value) { wardCode = value; }
    }
}
