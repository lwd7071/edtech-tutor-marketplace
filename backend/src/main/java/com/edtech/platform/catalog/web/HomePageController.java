package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomePageController {
    private static final Logger log = LoggerFactory.getLogger(HomePageController.class);
    private static final int FEATURED_TEACHER_COUNT = 4;
    private static final int FEATURED_SUBJECT_COUNT = 12;

    private final TeacherMarketplaceService teacherMarketplaceService;
    private final SubjectFacade subjectFacade;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activeNav", "home");
        List<SubjectSnapshot> subjects = loadSubjects(model);
        model.addAttribute("featuredSubjects", subjects);
        model.addAttribute("teacherCountsBySubject", loadTeacherCounts(subjects, model));
        model.addAttribute("featuredTeachers", loadTeachers(model));
        return "home/index";
    }

    private java.util.Map<java.util.UUID, Long> loadTeacherCounts(List<SubjectSnapshot> subjects, Model model) {
        try {
            return teacherMarketplaceService.countPublicTeachersBySubject(
                    subjects.stream().map(SubjectSnapshot::id).toList());
        } catch (RuntimeException ex) {
            log.warn("Public home subject counts are unavailable", ex);
            model.addAttribute("subjectCountsUnavailable", true);
            return java.util.Map.of();
        }
    }

    private List<SubjectSnapshot> loadSubjects(Model model) {
        try {
            return subjectFacade.getActiveSubjects().stream().limit(FEATURED_SUBJECT_COUNT).toList();
        } catch (RuntimeException ex) {
            log.warn("Public home subjects are unavailable", ex);
            model.addAttribute("subjectsUnavailable", true);
            return List.of();
        }
    }

    private List<TeacherCard> loadTeachers(Model model) {
        try {
            TeacherSearchParams params = new TeacherSearchParams(
                    null, null, null, null, null, null, null, null,
                    null, "rating_desc", null, null, 0, FEATURED_TEACHER_COUNT);
            Page<TeacherCard> page = teacherMarketplaceService.searchTeachers(params);
            return page.getContent();
        } catch (RuntimeException ex) {
            log.warn("Public home featured teachers are unavailable", ex);
            model.addAttribute("teachersUnavailable", true);
            return List.of();
        }
    }
}
