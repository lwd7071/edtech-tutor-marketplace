package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomePageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherMarketplaceService teacherMarketplaceService;
    @MockBean private SubjectFacade subjectFacade;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void rendersRealFeaturedSubjectsAndTeachers() throws Exception {
        UUID subjectId = UUID.randomUUID();
        TeacherCard teacher = new TeacherCard(UUID.randomUUID(), "Nguyễn Minh An", null,
                "Dạy Toán theo mục tiêu của học viên.", 5, true, true, false,
                List.of(), 350000, 4.8, 4.7, 12, 3);
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of(
                new SubjectSnapshot(subjectId, "MATH", "Toán học", "HIGH_SCHOOL", true)));
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tiến đúng mục tiêu.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trao đổi nhu cầu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/teachers\"")));
    }

    @Test
    void rendersHeaderNavigationWithBecomeATutorAndExploreLinks() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of()));

        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int navStart = html.indexOf("<nav class=\"public-nav\"");
        org.junit.jupiter.api.Assertions.assertTrue(navStart >= 0, "Phải tìm thấy thẻ <nav class=\"public-nav\"");
        int navEnd = html.indexOf("</nav>", navStart);
        org.junit.jupiter.api.Assertions.assertTrue(navEnd > navStart, "Phải tìm thấy thẻ đóng </nav>");
        String navContent = html.substring(navStart, navEnd);

        org.junit.jupiter.api.Assertions.assertTrue(navContent.contains("href=\"/become-a-tutor\""),
                "Thanh public-nav phải chứa liên kết /become-a-tutor");
        org.junit.jupiter.api.Assertions.assertTrue(navContent.contains("Trở thành gia sư"),
                "Thanh public-nav phải chứa nhãn Trở thành gia sư");
        org.junit.jupiter.api.Assertions.assertTrue(navContent.contains("href=\"/teachers\""),
                "Thanh public-nav phải chứa liên kết /teachers");
        org.junit.jupiter.api.Assertions.assertTrue(navContent.contains("Tìm gia sư"),
                "Thanh public-nav phải chứa nhãn Tìm gia sư");
        org.junit.jupiter.api.Assertions.assertTrue(navContent.contains("href=\"/ranking\""),
                "Thanh public-nav phải chứa liên kết /ranking");
        org.junit.jupiter.api.Assertions.assertTrue(navContent.contains("Bảng xếp hạng"),
                "Thanh public-nav phải chứa nhãn Bảng xếp hạng");
    }

    @Test
    void rendersPreplyStyleHeroWithBadgeAndLearningProgressVisual() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of()));

        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(html.contains("home-hero__badge"),
                "Hero phải chứa pill badge định vị giá trị 1-1");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("home-hero__mockup"),
                "Hero phải chứa mockup visual mô phỏng buổi học 1-1");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("home-hero__stat-card"),
                "Hero phải chứa thẻ thông số tiến độ học tập");
    }

    @Test
    void rendersHeroWith2DWindowFrameAndConnectingDoodles() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of()));

        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(html.contains("home-hero__window-frame"),
                "Hero mockup phải chứa khung cửa sổ 2D SVG");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("home-hero__doodle-connector"),
                "Hero mockup phải chứa đường nối tương tác 2D");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("home-hero__doodle-sparkle"),
                "Hero mockup phải chứa doodle sao lấp lánh");
    }

    @Test
    void showsPendingPriceWhenFeaturedTeacherHasNoActivePackage() throws Exception {
        TeacherCard teacher = new TeacherCard(UUID.randomUUID(), "Nguyễn Minh An", null,
                "Dạy Toán theo mục tiêu của học viên.", 5, true, true, false,
                List.of(), 0, 4.8, 4.7, 12, 3);
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Học phí đang cập nhật")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Từ 0 đ/buổi"))));
    }

    @Test
    void showsIndependentUnavailableStatesWhenPublicDataFails() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenThrow(new IllegalStateException("unavailable"));
        when(teacherMarketplaceService.searchTeachers(any())).thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh sách môn học chưa tải được.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh sách gia sư chưa tải được.")));
    }
}
