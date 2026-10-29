package com.edtech.platform.ranking.web;

import com.edtech.platform.ranking.dto.response.TeacherRankingItem;
import com.edtech.platform.ranking.service.TeacherStatsService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherRankingPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherRankingPageController.class);
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final TeacherStatsService teacherStatsService;
    private final SubjectFacade subjectFacade;

    @GetMapping("/ranking")
    public String ranking(
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size < 1 || size > MAX_SIZE ? DEFAULT_SIZE : size;
        UUID parsedSubjectId = parseSubjectId(subjectId);
        boolean invalidSubjectId = StringUtils.hasText(subjectId) && parsedSubjectId == null;

        model.addAttribute("activeNav", "ranking");
        model.addAttribute("subjectId", subjectId == null ? "" : subjectId);
        model.addAttribute("subjects", loadSubjects(model));
        model.addAttribute("invalidSubjectId", invalidSubjectId);
        model.addAttribute("pageSize", pageSize);

        Page<TeacherRankingItem> results = Page.empty(PageRequest.of(pageNumber, pageSize));
        boolean unavailable = false;
        if (!invalidSubjectId) {
            try {
                results = teacherStatsService.getGlobalRanking(
                        parsedSubjectId,
                        PageRequest.of(pageNumber, pageSize));
            } catch (RuntimeException ex) {
                log.error("Public teacher ranking page unavailable", ex);
                unavailable = true;
            }
        }

        model.addAttribute("results", results);
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("retryUrl", pageUrl(subjectId, pageNumber, pageSize));
        model.addAttribute("previousUrl", results.hasPrevious() ? pageUrl(subjectId, results.previousPageable().getPageNumber(), pageSize) : null);
        model.addAttribute("nextUrl", results.hasNext() ? pageUrl(subjectId, results.nextPageable().getPageNumber(), pageSize) : null);
        return "ranking/index";
    }

    private UUID parseSubjectId(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<SubjectSnapshot> loadSubjects(Model model) {
        try {
            return subjectFacade.getActiveSubjects();
        } catch (RuntimeException ex) {
            log.warn("Ranking subject filter unavailable", ex);
            model.addAttribute("subjectsUnavailable", true);
            return List.of();
        }
    }

    private String pageUrl(String subjectId, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/ranking");
        if (StringUtils.hasText(subjectId)) builder.queryParam("subjectId", subjectId);
        builder.queryParam("page", page).queryParam("size", size);
        return builder.build().encode().toUriString();
    }
}
