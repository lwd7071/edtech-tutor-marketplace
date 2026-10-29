package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.subject.facade.SubjectFacade;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherSearchPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherSearchPageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeacherMarketplaceService teacherMarketplaceService;

    @MockBean
    private SubjectFacade subjectFacade;

    @MockBean
    private AdministrativeLocationService locationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserStatusCacheService userStatusCacheService;

    @MockBean
    private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void rendersPublicTeacherResultsFromApplicationService() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of(new AdministrativeProvince("48", "Đà Nẵng")));

        mockMvc.perform(get("/teachers").param("keyword", "Toán"))
                .andExpect(status().isOk())
                .andExpect(view().name("teachers/search"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã xác minh")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tìm gia sư")));
    }

    @Test
    void invalidQueryRendersFieldErrorAndSkipsSearch() throws Exception {
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());
        String invalidKeyword = "x".repeat(101);

        mockMvc.perform(get("/teachers").param("keyword", invalidKeyword))
                .andExpect(status().isOk())
                .andExpect(view().name("teachers/search"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("không được vượt quá 100 ký tự")));

        verify(teacherMarketplaceService, never()).searchTeachers(any());
    }

    @Test
    void acceptsNativeGetFormWithBlankOptionalFilters() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers")
                        .param("keyword", "")
                        .param("subjectId", "")
                        .param("minPrice", "0")
                        .param("maxPrice", "")
                        .param("minRating", "")
                        .param("deliveryMode", "")
                        .param("provinceCode", "")
                        .param("dayOfWeek", "")
                        .param("startTime", "")
                        .param("endTime", "")
                        .param("sort", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("teachers/search"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Kiểm tra lại bộ lọc"))));

        verify(teacherMarketplaceService).searchTeachers(any());
    }

    @Test
    void teacherCardShouldRenderDualActionButtonStack() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("teacher-card__actions")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("button button--primary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xem hồ sơ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("button button--secondary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhắn tin")));
    }

    @Test
    void showsPendingPriceWhenTeacherHasNoActivePackage() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 0, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Học phí đang cập nhật")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("0 ₫ / buổi"))));
    }

    @Test
    void removesHeroBannerAndRendersCompactPageHeader() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("marketplace-intro"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("cho chặng học tiếp theo"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("teachers-page-header")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gia sư cho mục tiêu của bạn")));
    }

    @Test
    void rendersHorizontalTopFilterBarWithKeyFilters() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("teachers-top-filter")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"keyword\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"subjectId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"deliveryMode\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"minPrice\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"maxPrice\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"minRating\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"provinceCode\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"sort\"")));
    }

    @Test
    void removesSidebarAndRendersFullWidthResultsLayout() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("filter-panel"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("teachers-results-wrapper")));
    }
}
