package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.AdministrativeWard;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.MultiValueMap;

import java.time.DayOfWeek;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TeacherSearchPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherSearchPageController.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final TeacherMarketplaceService teacherMarketplaceService;
    private final SubjectFacade subjectFacade;
    private final AdministrativeLocationService locationService;

    @GetMapping("/teachers")
    public String search(
            @Valid @ModelAttribute("filters") TeacherSearchParams filters,
            BindingResult bindingResult,
            @RequestParam MultiValueMap<String, String> rawQuery,
            HttpServletRequest request,
            Model model) {
        model.addAttribute("activeNav", "teachers");
        int pageNumber = filters.page() == null || filters.page() < 0 ? 0 : filters.page();
        int pageSize = filters.size() == null || filters.size() < 1 || filters.size() > 100
                ? DEFAULT_PAGE_SIZE : filters.size();

        model.addAttribute("subjects", loadSubjects());
        model.addAttribute("provinces", loadProvinces());
        model.addAttribute("wards", loadWards(filters.provinceCode()));
        model.addAttribute("days", dayOptions());
        model.addAttribute("rawQuery", rawQuery);
        model.addAttribute("retryUrl", retryUrl(request));
        model.addAttribute("filterError", bindingResult.hasErrors());

        Page<TeacherCard> results = Page.empty(PageRequest.of(pageNumber, pageSize));
        boolean unavailable = false;
        if (!bindingResult.hasErrors()) {
            try {
                results = teacherMarketplaceService.searchTeachers(filters);
            } catch (RuntimeException ex) {
                log.error("Teacher search page unavailable", ex);
                unavailable = true;
            }
        }

        model.addAttribute("results", results);
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("previousUrl", results.hasPrevious() ? pageUrl(filters, results.previousPageable().getPageNumber()) : null);
        model.addAttribute("nextUrl", results.hasNext() ? pageUrl(filters, results.nextPageable().getPageNumber()) : null);
        return "teachers/search";
    }

    private List<SubjectSnapshot> loadSubjects() {
        try {
            return subjectFacade.getActiveSubjects();
        } catch (RuntimeException ex) {
            log.warn("Teacher search subject filters unavailable", ex);
            return List.of();
        }
    }

    private List<AdministrativeProvince> loadProvinces() {
        try {
            return locationService.getProvinces();
        } catch (RuntimeException ex) {
            log.warn("Teacher search location filters unavailable", ex);
            return List.of();
        }
    }

    private List<AdministrativeWard> loadWards(String provinceCode) {
        if (provinceCode == null || !provinceCode.matches("^[0-9]{2}$")) return List.of();
        try {
            return locationService.getWards(provinceCode);
        } catch (RuntimeException ex) {
            log.warn("Teacher search ward filters unavailable", ex);
            return List.of();
        }
    }

    private String pageUrl(TeacherSearchParams filters, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/teachers");
        addIfPresent(builder, "keyword", filters.keyword());
        addIfPresent(builder, "subjectId", filters.subjectId());
        addIfPresent(builder, "dayOfWeek", filters.dayOfWeek());
        addIfPresent(builder, "startTime", filters.startTime());
        addIfPresent(builder, "endTime", filters.endTime());
        addIfPresent(builder, "minPrice", filters.minPrice());
        addIfPresent(builder, "maxPrice", filters.maxPrice());
        addIfPresent(builder, "minRating", filters.minRating());
        addIfPresent(builder, "deliveryMode", filters.deliveryMode());
        addIfPresent(builder, "sort", filters.sort());
        addIfPresent(builder, "provinceCode", filters.provinceCode());
        addIfPresent(builder, "wardCode", filters.wardCode());
        builder.queryParam("page", page).queryParam("size", filters.size() == null ? DEFAULT_PAGE_SIZE : filters.size());
        return builder.build().encode().toUriString();
    }

    private static void addIfPresent(UriComponentsBuilder builder, String name, Object value) {
        if (value != null && !value.toString().isBlank()) builder.queryParam(name, value);
    }

    private String retryUrl(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getRequestURI() + (query == null ? "" : "?" + query);
    }

    private List<DayOption> dayOptions() {
        return List.of(
                new DayOption(DayOfWeek.MONDAY, "Thứ hai"),
                new DayOption(DayOfWeek.TUESDAY, "Thứ ba"),
                new DayOption(DayOfWeek.WEDNESDAY, "Thứ tư"),
                new DayOption(DayOfWeek.THURSDAY, "Thứ năm"),
                new DayOption(DayOfWeek.FRIDAY, "Thứ sáu"),
                new DayOption(DayOfWeek.SATURDAY, "Thứ bảy"),
                new DayOption(DayOfWeek.SUNDAY, "Chủ nhật"));
    }

    public record DayOption(DayOfWeek value, String label) {}
}
