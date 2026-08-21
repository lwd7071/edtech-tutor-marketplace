package com.edtech.platform.ranking.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.ranking.dto.response.TeacherRankingItem;
import com.edtech.platform.ranking.dto.response.TeacherStatsView;
import com.edtech.platform.ranking.service.TeacherStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeacherStatsController {

    private final TeacherStatsService teacherStatsService;

    @GetMapping("/api/teacher/stats")
    public TeacherStatsView getTeacherStats(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return teacherStatsService.getTeacherStats(authenticatedUser.id());
    }

    @GetMapping("/api/public/teachers/ranking")
    public Page<TeacherRankingItem> getGlobalRanking(
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            Pageable pageable) {
        return teacherStatsService.getGlobalRanking(subjectId, pageable);
    }
}
