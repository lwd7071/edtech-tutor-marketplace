package com.edtech.platform.ranking.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.ranking.dto.response.TeacherRankingItem;
import com.edtech.platform.ranking.dto.response.TeacherStatsView;
import com.edtech.platform.ranking.service.TeacherStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class TeacherStatsController {

    private final TeacherStatsService teacherStatsService;

    @GetMapping("/api/teacher/stats")
    public ApiResponse<TeacherStatsView> getTeacherStats(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.ok(teacherStatsService.getTeacherStats(authenticatedUser.id()));
    }

    @GetMapping("/api/public/teachers/ranking")
    public ApiResponse<java.util.List<TeacherRankingItem>> getGlobalRanking(
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<TeacherRankingItem> resultPage = teacherStatsService.getGlobalRanking(subjectId, pageRequest);
        return ApiResponse.page(resultPage.getContent(), PageMeta.from(resultPage));
    }
}
