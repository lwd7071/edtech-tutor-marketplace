package com.edtech.platform.ranking.web;

import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.ranking.dto.response.TeacherRankingItem;
import com.edtech.platform.ranking.service.TeacherStatsService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherRankingPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherRankingPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherStatsService teacherStatsService;
    @MockBean private SubjectFacade subjectFacade;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void rendersRankedTeachersWithRealStats() throws Exception {
        UUID subjectId = UUID.randomUUID();
        TeacherRankingItem teacher = TeacherRankingItem.builder()
                .teacherId(UUID.randomUUID()).fullName("Nguyễn Minh An").avatarUrl(null)
                .bioExcerpt("Dạy Toán theo mục tiêu của học viên.")
                .bayesianRating(new BigDecimal("4.82")).completedSessionCount(24).globalRank(1).build();
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of(
                new SubjectSnapshot(subjectId, "MATH", "Toán học", "HIGH_SCHOOL", true)));
        when(teacherStatsService.getGlobalRanking(eq(subjectId), any())).thenReturn(new PageImpl<>(List.of(teacher)));

        mockMvc.perform(get("/ranking").header("Accept-Language", "vi-VN").param("subjectId", subjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("ranking/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("24 buổi học đã hoàn tất")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("4,82 điểm")));
    }

    @Test
    void invalidSubjectFilterShowsRecoveryAndSkipsRankingQuery() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());

        mockMvc.perform(get("/ranking").param("subjectId", "not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(view().name("ranking/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Môn học không hợp lệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xem toàn bộ bảng xếp hạng")));

        verify(teacherStatsService, never()).getGlobalRanking(any(), any());
    }

    @Test
    void serviceFailureRendersRetryState() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(teacherStatsService.getGlobalRanking(isNull(), any())).thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/ranking"))
                .andExpect(status().isOk())
                .andExpect(view().name("ranking/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được bảng xếp hạng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/ranking?page=0&amp;size=20\">Thử lại</a>")));
    }

    @Test
    void rendersPublicNavWithActiveRankingLinkAndAriaCurrent() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(teacherStatsService.getGlobalRanking(isNull(), any())).thenReturn(new PageImpl<>(List.of()));

        String html = mockMvc.perform(get("/ranking"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int navStart = html.indexOf("<nav class=\"public-nav\"");
        org.junit.jupiter.api.Assertions.assertTrue(navStart >= 0, "Phải tìm thấy thẻ <nav class=\"public-nav\"");
        int navEnd = html.indexOf("</nav>", navStart);
        org.junit.jupiter.api.Assertions.assertTrue(navEnd > navStart, "Phải tìm thấy thẻ đóng </nav>");
        String navContent = html.substring(navStart, navEnd);

        org.junit.jupiter.api.Assertions.assertTrue(
                navContent.contains("href=\"/ranking\"") && navContent.contains("public-nav__link--active") && navContent.contains("aria-current=\"page\""),
                "Thanh public-nav trên trang /ranking phải kích hoạt active state và aria-current cho liên kết /ranking");
    }
}
