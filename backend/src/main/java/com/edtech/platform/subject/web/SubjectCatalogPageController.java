package com.edtech.platform.subject.web;

import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.dto.SubjectSummary;
import com.edtech.platform.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SubjectCatalogPageController {
    private static final Logger log = LoggerFactory.getLogger(SubjectCatalogPageController.class);
    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 30;

    private final SubjectService subjectService;

    @GetMapping("/subjects")
    public String subjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String educationLevel,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size < 1 || size > MAX_SIZE ? DEFAULT_SIZE : size;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        EducationLevel level = parseLevel(educationLevel);
        boolean invalidLevel = StringUtils.hasText(educationLevel) && level == null;
        boolean invalidKeyword = normalizedKeyword.length() > 100;
        boolean filterError = invalidLevel || invalidKeyword;

        model.addAttribute("activeNav", "subjects");
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("educationLevel", educationLevel == null ? "" : educationLevel);
        model.addAttribute("educationLevels", levelOptions());
        model.addAttribute("filterError", filterError);
        model.addAttribute("invalidLevel", invalidLevel);
        model.addAttribute("invalidKeyword", invalidKeyword);
        model.addAttribute("pageSize", pageSize);

        Page<SubjectSummary> results = Page.empty(PageRequest.of(pageNumber, pageSize, Sort.by("name").ascending()));
        boolean unavailable = false;
        if (!filterError) {
            try {
                results = subjectService.getPublicSubjects(
                        normalizedKeyword,
                        level,
                        PageRequest.of(pageNumber, pageSize, Sort.by("name").ascending()));
            } catch (RuntimeException ex) {
                log.error("Public subject catalog page unavailable", ex);
                unavailable = true;
            }
        }

        model.addAttribute("results", results);
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("previousUrl", results.hasPrevious() ? pageUrl(normalizedKeyword, educationLevel, results.previousPageable().getPageNumber(), pageSize) : null);
        model.addAttribute("nextUrl", results.hasNext() ? pageUrl(normalizedKeyword, educationLevel, results.nextPageable().getPageNumber(), pageSize) : null);
        return "subjects/index";
    }

    private EducationLevel parseLevel(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return EducationLevel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<LevelOption> levelOptions() {
        return List.of(
                new LevelOption(EducationLevel.ELEMENTARY, "Tiểu học"),
                new LevelOption(EducationLevel.MIDDLE_SCHOOL, "Trung học cơ sở"),
                new LevelOption(EducationLevel.HIGH_SCHOOL, "Trung học phổ thông"),
                new LevelOption(EducationLevel.UNIVERSITY, "Đại học"),
                new LevelOption(EducationLevel.OTHER, "Khác"));
    }

    private String pageUrl(String keyword, String educationLevel, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/subjects");
        if (StringUtils.hasText(keyword)) builder.queryParam("keyword", keyword);
        if (StringUtils.hasText(educationLevel)) builder.queryParam("educationLevel", educationLevel);
        builder.queryParam("page", page).queryParam("size", size);
        return builder.build().encode().toUriString();
    }

    public record LevelOption(EducationLevel value, String label) {}
}
