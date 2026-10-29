package com.edtech.platform.teacher.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.AdministrativeWard;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.service.TeacherProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherProfilePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherProfilePageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherProfileService profiles;
    @MockBean private AdministrativeLocationService locations;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(profiles.getProfile(teacherId)).thenReturn(profile(ProfileStatus.DRAFT));
        when(locations.getProvinces()).thenReturn(List.of(new AdministrativeProvince("01", "Hà Nội")));
        when(locations.getWards("01")).thenReturn(List.of(new AdministrativeWard("00001", "01", "Phường ví dụ")));
    }

    @Test
    void rendersProfileAndNativeGetResidenceSelector() throws Exception {
        mockMvc.perform(get("/teacher/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giới thiệu và phương pháp giảng dạy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"get\" action=\"/teacher/profile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gửi xét duyệt")));
        verify(profiles).getProfile(teacherId);
        verify(locations).getWards("01");
    }

    @Test
    void profileUpdateUsesServiceAndPostRedirectGet() throws Exception {
        mockMvc.perform(post("/teacher/profile/update").with(csrf())
                        .param("bio", "Dạy Toán theo từng bước")
                        .param("yearsOfExperience", "5")
                        .param("languagesCsv", "Tiếng Việt, Tiếng Anh")
                        .param("supportsOnline", "true").param("supportsOffline", "false")
                        .param("introductionVideoUrl", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/teacher/profile"));
        verify(profiles).updateProfile(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.argThat(request ->
                request.bio().equals("Dạy Toán theo từng bước") && request.languages().equals(List.of("Tiếng Việt", "Tiếng Anh"))));
    }

    @Test
    void invalidProfileDoesNotCallServiceAndKeepsFieldError() throws Exception {
        mockMvc.perform(post("/teacher/profile/update").with(csrf())
                        .param("bio", "<script>").param("yearsOfExperience", "81")
                        .param("languagesCsv", "Tiếng Việt").param("supportsOnline", "true")
                        .param("supportsOffline", "false").param("introductionVideoUrl", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("không được chứa thẻ HTML")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tối đa là 80")));
        verify(profiles, never()).updateProfile(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void residenceUpdateChecksWardBelongsToSelectedProvince() throws Exception {
        mockMvc.perform(post("/teacher/profile/residence").with(csrf())
                        .param("provinceCode", "01").param("wardCode", "00001"))
                .andExpect(status().is3xxRedirection());
        verify(profiles).updateResidence(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.argThat(request ->
                "01".equals(request.provinceCode()) && "00001".equals(request.wardCode())));
    }

    @Test
    void submittingDraftUsesProfileService() throws Exception {
        mockMvc.perform(post("/teacher/profile/submit").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(profiles).submitProfile(teacherId);
    }

    @Test
    void pendingProfileCannotEditTeachingContentButCanStillUpdateResidence() throws Exception {
        when(profiles.getProfile(teacherId)).thenReturn(profile(ProfileStatus.PENDING_APPROVAL));
        mockMvc.perform(get("/teacher/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu khu vực")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Gửi xét duyệt"))));
    }

    private TeacherProfileDetail profile(ProfileStatus status) {
        return new TeacherProfileDetail(UUID.randomUUID(), "Dạy Toán theo từng bước", 5, List.of("Tiếng Việt"),
                true, false, null, "01", "00001", null, status, null, false, false);
    }
}
