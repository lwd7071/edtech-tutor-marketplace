package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.ranking.facade.PublicReviewFacade;
import com.edtech.platform.ranking.facade.dto.PublicReviewSnapshot;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.facade.TeacherFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Controller("publicTeacherProfilePageController")
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherProfilePageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherProfilePageController.class);
    private static final int SECTION_PAGE_SIZE = 6;

    private final TeacherMarketplaceService teacherMarketplaceService;
    private final PricingPackageService pricingPackageService;
    private final TeacherFacade teacherFacade;
    private final PublicReviewFacade reviewFacade;
    private final SubjectFacade subjectFacade;
    private final java.time.Clock clock;

    @GetMapping("/{id}")
    public String profile(@PathVariable UUID id, Model model, HttpServletResponse response,
                          @AuthenticationPrincipal AuthenticatedUser user,
                          @RequestParam(required = false) UUID packageId) {
        model.addAttribute("activeNav", "teachers");
        model.addAttribute("studentCanMessage", user != null && "STUDENT".equals(user.role()));
        model.addAttribute("guestCanMessage", user == null);
        boolean studentCanPurchase = user != null && "STUDENT".equals(user.role());
        model.addAttribute("studentCanPurchase", studentCanPurchase);
        model.addAttribute("guestCanRequestTrial", user == null);
        model.addAttribute("studentCanRequestTrial", studentCanPurchase);
        model.addAttribute("messageTeacherId", id);
        if (user != null) response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        TeacherPublicDetail teacher;
        try {
            teacher = teacherMarketplaceService.getTeacherDetail(id);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND
                    || ex.getErrorCode() == ErrorCode.TEACHER_PROFILE_NOT_FOUND) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return "teachers/not-found";
            }
            log.warn("Unable to load public teacher profile id={}", id, ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return "teachers/profile-unavailable";
        } catch (RuntimeException ex) {
            log.error("Unable to load public teacher profile id={}", id, ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return "teachers/profile-unavailable";
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("trialSubjectsUnavailable", false);
        model.addAttribute("trialSubjects", studentCanPurchase ? loadTrialSubjects(id, model) : List.of());
        model.addAttribute("trialMinimumLocalTime", java.time.LocalDateTime.ofInstant(
                clock.instant(), java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("packagesUnavailable", false);
        model.addAttribute("availabilityUnavailable", false);
        model.addAttribute("reviewsUnavailable", false);
        List<PricingPackageView> packages = loadPackages(id, model);
        model.addAttribute("packages", packages);
        model.addAttribute("selectedPackageId", packages.stream().anyMatch(item -> item.id().equals(packageId)) ? packageId : null);
        model.addAttribute("purchaseReturnTo", "/teachers/" + id);
        Map<UUID, UUID> purchaseKeys = new LinkedHashMap<>();
        if (studentCanPurchase) packages.forEach(item -> purchaseKeys.put(item.id(), UUID.randomUUID()));
        model.addAttribute("purchaseKeys", purchaseKeys);
        model.addAttribute("availability", loadAvailability(id, model));
        model.addAttribute("reviews", loadReviews(id, model));
        return "teachers/profile";
    }

    private List<TrialSubjectOption> loadTrialSubjects(UUID teacherId, Model model) {
        try {
            List<UUID> assignedIds = teacherFacade.getSubjectIdsForTeacher(teacherId);
            if (assignedIds.isEmpty()) return List.of();
            Map<UUID, SubjectSnapshot> subjects = subjectFacade.getSubjects(assignedIds);
            return assignedIds.stream().map(subjects::get).filter(java.util.Objects::nonNull)
                    .filter(SubjectSnapshot::isActive)
                    .map(subject -> new TrialSubjectOption(subject.id(), subject.name())).toList();
        } catch (RuntimeException ex) {
            log.warn("Unable to load trial subjects for teacher id={}", teacherId, ex);
            model.addAttribute("trialSubjectsUnavailable", true);
            return List.of();
        }
    }

    private List<PricingPackageView> loadPackages(UUID teacherId, Model model) {
        try {
            Page<PricingPackageView> page = pricingPackageService.getTeacherPackages(
                    teacherId, PageRequest.of(0, SECTION_PAGE_SIZE));
            return page.getContent();
        } catch (RuntimeException ex) {
            log.warn("Unable to load public packages for teacher id={}", teacherId, ex);
            model.addAttribute("packagesUnavailable", true);
            return List.of();
        }
    }

    private List<AvailabilityLine> loadAvailability(UUID teacherId, Model model) {
        try {
            return teacherFacade.getPublicAvailability(teacherId).stream()
                    .map(slot -> new AvailabilityLine(dayLabel(slot.dayOfWeek()), slot.startTime(), slot.endTime(), slot.timezone()))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Unable to load public availability for teacher id={}", teacherId, ex);
            model.addAttribute("availabilityUnavailable", true);
            return List.of();
        }
    }

    private List<PublicReviewSnapshot> loadReviews(UUID teacherId, Model model) {
        try {
            Page<PublicReviewSnapshot> page = reviewFacade.getPublicReviews(
                    teacherId, PageRequest.of(0, SECTION_PAGE_SIZE));
            return page.getContent();
        } catch (RuntimeException ex) {
            log.warn("Unable to load public reviews for teacher id={}", teacherId, ex);
            model.addAttribute("reviewsUnavailable", true);
            return List.of();
        }
    }

    private String dayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Thứ hai";
            case TUESDAY -> "Thứ ba";
            case WEDNESDAY -> "Thứ tư";
            case THURSDAY -> "Thứ năm";
            case FRIDAY -> "Thứ sáu";
            case SATURDAY -> "Thứ bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }

    public record AvailabilityLine(String dayLabel, LocalTime startTime, LocalTime endTime, String timezone) {}
    public record TrialSubjectOption(UUID id, String name) {}
}
