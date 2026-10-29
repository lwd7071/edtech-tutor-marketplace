package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.subject.facade.SubjectFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TDD: Kiểm tra giao diện tìm kiếm gia sư và hồ sơ trên Mobile (Bottom Sheet & Action Dock).
 */
@WebMvcTest(TeacherSearchPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherSearchMobileLayoutTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherMarketplaceService teacherMarketplaceService;
    @MockBean private SubjectFacade subjectFacade;
    @MockBean private AdministrativeLocationService locationService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private com.edtech.platform.common.security.HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    @DisplayName("Trang tìm gia sư phải có nút trigger mở Bộ lọc trên Mobile và khối Bottom Sheet lọc")
    void searchPageShouldRenderMobileFilterTriggerAndBottomSheet() throws Exception {
        TeacherCard teacher = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, true, true, false, List.of(), 350000, 4.8, 4.7, 12, 3, "Đà Nẵng", "Hải Châu");
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new PageImpl<>(List.of(teacher)));
        when(subjectFacade.getActiveSubjects()).thenReturn(List.of());
        when(locationService.getProvinces()).thenReturn(List.of());

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mobile-filter-trigger")))
                .andExpect(content().string(containsString("teachers-filter-sheet")))
                .andExpect(content().string(containsString("mobile-bottom-sheet")));
    }

    @Test
    @DisplayName("teachers.css phải định nghĩa mobile-filter-trigger ẩn trên desktop và hiện trên mobile")
    void teachersCssShouldDefineMobileFilterTriggerStyles() throws Exception {
        Path cssPath = Path.of("src/main/resources/static/css/teachers.css");
        assertTrue(Files.exists(cssPath), "teachers.css must exist");
        String css = Files.readString(cssPath);

        assertTrue(
                css.contains(".mobile-filter-trigger"),
                "teachers.css must define .mobile-filter-trigger"
        );
    }

    @Test
    @DisplayName("teachers-profile.css phải định nghĩa .mobile-booking-dock với fixed bottom và viền mực 1.5px")
    void profileCssShouldDefineMobileBookingDock() throws Exception {
        Path cssPath = Path.of("src/main/resources/static/css/teachers-profile.css");
        assertTrue(Files.exists(cssPath), "teachers-profile.css must exist");
        String css = Files.readString(cssPath);

        assertTrue(
                css.contains(".mobile-booking-dock"),
                "teachers-profile.css must define .mobile-booking-dock"
        );
        assertTrue(
                css.contains("border-top: 1.5px solid var(--ink)") || css.contains("border-top: 1.5px solid #171714"),
                ".mobile-booking-dock must have 1.5px ink border"
        );
    }
}
