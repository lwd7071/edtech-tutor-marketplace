package com.edtech.platform.teacher.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.AvailabilityItem;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.dto.ReplaceAvailabilityRequest;
import com.edtech.platform.teacher.service.TeacherAvailabilityService;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TeacherAvailabilityPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherAvailabilityPageController.class);
    private static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";
    private final TeacherAvailabilityService availability;

    @GetMapping("/teacher/availability")
    public String page(@AuthenticationPrincipal AuthenticatedUser user, Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "availability");
        addDayOptions(model);
        try {
            List<SlotForm> items = availability.getAvailabilities(user.id()).stream().map(SlotForm::from).toList();
            model.addAttribute("availabilityForm", new AvailabilityForm(new ArrayList<>(items)));
            model.addAttribute("availabilityUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher availability could not be loaded");
            model.addAttribute("availabilityForm", new AvailabilityForm());
            model.addAttribute("availabilityUnavailable", true);
        }
        return "workspace/teacher-availability";
    }

    @PostMapping("/teacher/availability")
    public String submit(@AuthenticationPrincipal AuthenticatedUser user,
                         @ModelAttribute("availabilityForm") AvailabilityForm form, BindingResult binding,
                         @RequestParam(defaultValue = "save") String action,
                         Model model, RedirectAttributes redirect, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        if (form.getItems() == null) form.setItems(new ArrayList<>());
        if (binding.hasErrors()) {
            form.setFormError("Có giá trị thời gian không hợp lệ. Kiểm tra lại các khung giờ.");
            return renderForm(user, form, model);
        }
        if ("add".equals(action)) {
            form.getItems().add(new SlotForm());
            return renderForm(user, form, model);
        }
        if (action.startsWith("remove:")) {
            try {
                int removeIndex = Integer.parseInt(action.substring("remove:".length()));
                if (removeIndex >= 0 && removeIndex < form.getItems().size()) form.getItems().remove(removeIndex);
            } catch (NumberFormatException ignored) { /* Ignore malformed row action and preserve the form. */ }
            return renderForm(user, form, model);
        }

        ReplaceAvailabilityRequest request = toRequest(form);
        if (request == null) return renderForm(user, form, model);
        try {
            availability.replaceAvailabilities(user.id(), request);
            redirect.addFlashAttribute("availabilityNotice", "Đã lưu lịch rảnh.");
            return "redirect:/teacher/availability";
        } catch (BusinessException ex) {
            form.setFormError(errorMessage(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher availability could not be saved");
            form.setFormError("Chưa lưu được lịch rảnh. Lịch hiện tại vẫn được giữ nguyên; hãy tải lại rồi thử lại.");
        }
        return renderForm(user, form, model);
    }

    private String renderForm(AuthenticatedUser user, AvailabilityForm form, Model model) {
        model.addAttribute("activeWorkspaceNav", "availability");
        model.addAttribute("availabilityForm", form);
        model.addAttribute("availabilityUnavailable", false);
        addDayOptions(model);
        return "workspace/teacher-availability";
    }

    private ReplaceAvailabilityRequest toRequest(AvailabilityForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<AvailabilityItem> items = new ArrayList<>();
        for (int index = 0; index < form.getItems().size(); index++) {
            SlotForm slot = form.getItems().get(index);
            slot.clearErrors();
            DayOfWeek day = parseDay(slot, index, errors);
            LocalTime start = parseTime(slot.getStartTime(), "startTime", "Nhập giờ bắt đầu.", slot, index, errors);
            LocalTime end = parseTime(slot.getEndTime(), "endTime", "Nhập giờ kết thúc.", slot, index, errors);
            String timezone = slot.getTimezone() == null || slot.getTimezone().isBlank() ? DEFAULT_TIME_ZONE : slot.getTimezone().trim();
            boolean active = !"false".equalsIgnoreCase(slot.getActive());
            if (slot.getActive() != null && !slot.getActive().isBlank() && !List.of("true", "false").contains(slot.getActive().toLowerCase())) {
                slot.setActiveError("Chọn trạng thái khung giờ.");
                errors.put("items[" + index + "].active", slot.getActiveError());
            }
            try { ZoneId.of(timezone); }
            catch (RuntimeException ex) {
                slot.setTimezoneError("Nhập múi giờ hợp lệ, ví dụ Asia/Ho_Chi_Minh.");
                errors.put("items[" + index + "].timezone", slot.getTimezoneError());
            }
            if (day != null && start != null && end != null)
                items.add(new AvailabilityItem(day, start, end, timezone, active));
        }
        if (!errors.isEmpty()) {
            form.setFormError("Có khung giờ chưa hợp lệ. Sửa các trường được đánh dấu rồi lưu lại.");
            return null;
        }
        return new ReplaceAvailabilityRequest(items);
    }

    private DayOfWeek parseDay(SlotForm slot, int index, Map<String, String> errors) {
        if (slot.getDayOfWeek() == null || slot.getDayOfWeek().isBlank()) {
            slot.setDayError("Chọn ngày trong tuần.");
            errors.put("items[" + index + "].dayOfWeek", slot.getDayError());
            return null;
        }
        try { return DayOfWeek.valueOf(slot.getDayOfWeek()); }
        catch (IllegalArgumentException ex) {
            slot.setDayError("Ngày trong tuần không hợp lệ.");
            errors.put("items[" + index + "].dayOfWeek", slot.getDayError());
            return null;
        }
    }

    private LocalTime parseTime(String value, String field, String requiredMessage, SlotForm slot,
                                int index, Map<String, String> errors) {
        String error = null;
        if (value == null || value.isBlank()) error = requiredMessage;
        else {
            try { return LocalTime.parse(value); }
            catch (DateTimeParseException ex) { error = "Nhập giờ theo định dạng HH:mm."; }
        }
        if ("startTime".equals(field)) slot.setStartError(error); else slot.setEndError(error);
        errors.put("items[" + index + "]." + field, error);
        return null;
    }

    private String errorMessage(ErrorCode code) {
        return switch (code) {
            case AVAILABILITY_TIME_CONFLICT -> "Các khung giờ đang hoạt động bị trùng nhau. Điều chỉnh thời gian rồi lưu lại.";
            case AVAILABILITY_INVALID_RANGE -> "Giờ kết thúc cần sau giờ bắt đầu.";
            case AVAILABILITY_INVALID_TIMEZONE -> "Múi giờ không hợp lệ. Dùng tên múi giờ như Asia/Ho_Chi_Minh.";
            case TEACHER_PROFILE_NOT_FOUND -> "Chưa tìm thấy hồ sơ gia sư của bạn.";
            default -> "Chưa lưu được lịch rảnh. Tải lại trang rồi thử lại.";
        };
    }

    private void addDayOptions(Model model) {
        model.addAttribute("dayOptions", List.of(
                new DayOption("MONDAY", "Thứ Hai"), new DayOption("TUESDAY", "Thứ Ba"),
                new DayOption("WEDNESDAY", "Thứ Tư"), new DayOption("THURSDAY", "Thứ Năm"),
                new DayOption("FRIDAY", "Thứ Sáu"), new DayOption("SATURDAY", "Thứ Bảy"),
                new DayOption("SUNDAY", "Chủ Nhật")));
    }

    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record DayOption(String value, String label) {}

    public static class AvailabilityForm {
        private List<SlotForm> items = new ArrayList<>();
        private String formError;
        public AvailabilityForm() {}
        public AvailabilityForm(List<SlotForm> items) { this.items = items; }
        public List<SlotForm> getItems() { return items; } public void setItems(List<SlotForm> value) { items = value; }
        public String getFormError() { return formError; } public void setFormError(String value) { formError = value; }
    }

    public static class SlotForm {
        private String dayOfWeek; private String startTime; private String endTime;
        private String timezone = DEFAULT_TIME_ZONE; private String active = "true";
        private String dayError; private String startError; private String endError; private String timezoneError; private String activeError;
        static SlotForm from(AvailabilityView view) {
            SlotForm slot = new SlotForm(); slot.dayOfWeek = view.dayOfWeek().name();
            slot.startTime = view.startTime().toString(); slot.endTime = view.endTime().toString();
            slot.timezone = view.timezone(); slot.active = Boolean.toString(view.isActive()); return slot;
        }
        void clearErrors() { dayError = null; startError = null; endError = null; timezoneError = null; activeError = null; }
        public String getDayOfWeek() { return dayOfWeek; } public void setDayOfWeek(String value) { dayOfWeek = value; }
        public String getStartTime() { return startTime; } public void setStartTime(String value) { startTime = value; }
        public String getEndTime() { return endTime; } public void setEndTime(String value) { endTime = value; }
        public String getTimezone() { return timezone; } public void setTimezone(String value) { timezone = value; }
        public String getActive() { return active; } public void setActive(String value) { active = value; }
        public String getDayError() { return dayError; } public void setDayError(String value) { dayError = value; }
        public String getStartError() { return startError; } public void setStartError(String value) { startError = value; }
        public String getEndError() { return endError; } public void setEndError(String value) { endError = value; }
        public String getTimezoneError() { return timezoneError; } public void setTimezoneError(String value) { timezoneError = value; }
        public String getActiveError() { return activeError; } public void setActiveError(String value) { activeError = value; }
    }
}
