package com.edtech.platform.ranking.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.ranking.dto.response.TeacherStatsView;
import com.edtech.platform.ranking.service.TeacherStatsService;
import com.edtech.platform.teacher.facade.TeacherFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherStatsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherStatsPageController.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final TeacherStatsService statistics;
    private final TeacherFacade teachers;

    @GetMapping("/teacher/stats")
    public String page(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        requireTeacher(user);
        model.addAttribute("activeWorkspaceNav", "stats");
        model.addAttribute("stats", null);
        model.addAttribute("statsEmpty", false);
        model.addAttribute("statsUnavailable", false);
        try {
            UUID teacherId = teachers.getTeacherByUserId(user.id()).id();
            TeacherStatsView stats = statistics.getTeacherStats(teacherId);
            model.addAttribute("stats", stats);
            model.addAttribute("completionRateLabel", formatPercent(stats.getCompletionRate()));
            model.addAttribute("trialConversionRateLabel", formatPercent(stats.getTrialConversionRate()));
            model.addAttribute("calculatedAtLabel", stats.getCalculatedAt() == null
                    ? "Chưa có thời gian cập nhật" : DATE_TIME.format(stats.getCalculatedAt()));
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.TEACHER_STATS_NOT_FOUND) {
                model.addAttribute("statsEmpty", true);
            } else {
                log.warn("Teacher statistics are unavailable");
                model.addAttribute("statsUnavailable", true);
            }
        } catch (RuntimeException ex) {
            log.warn("Teacher statistics are unavailable");
            model.addAttribute("statsUnavailable", true);
        }
        return "workspace/teacher-stats";
    }

    private String formatPercent(java.math.BigDecimal value) {
        if (value == null) return "—";
        NumberFormat format = NumberFormat.getPercentInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(1);
        return format.format(value);
    }

    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }
}
