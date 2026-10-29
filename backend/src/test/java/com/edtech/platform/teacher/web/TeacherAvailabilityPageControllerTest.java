package com.edtech.platform.teacher.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.teacher.dto.AvailabilityItem;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.dto.ReplaceAvailabilityRequest;
import com.edtech.platform.teacher.service.TeacherAvailabilityService;
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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherAvailabilityPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherAvailabilityPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherAvailabilityService availability;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
    }

    @Test
    void getRendersScheduleAndUsesPrivateNoStoreResponse() throws Exception {
        when(availability.getAvailabilities(teacherId)).thenReturn(List.of(slot(DayOfWeek.MONDAY, "19:00", "20:30", true)));
        mockMvc.perform(get("/teacher/availability"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-availability"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thứ Hai")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("19:00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Asia/Ho_Chi_Minh")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void emptyScheduleExplainsHowToAddFirstSlot() throws Exception {
        when(availability.getAvailabilities(teacherId)).thenReturn(List.of());
        mockMvc.perform(get("/teacher/availability"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có khung giờ nào")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thêm khung giờ")));
    }

    @Test
    void addRowPreservesExistingValuesWithoutCallingService() throws Exception {
        mockMvc.perform(post("/teacher/availability").with(csrf()).param("action", "add")
                        .param("items[0].dayOfWeek", "MONDAY").param("items[0].startTime", "19:00")
                        .param("items[0].endTime", "20:00").param("items[0].timezone", "Asia/Ho_Chi_Minh")
                        .param("items[0].active", "true"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-availability"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"MONDAY\" selected")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"items[1].dayOfWeek\"")));
    }

    @Test
    void removeRowUsesButtonIndexAndKeepsOtherRows() throws Exception {
        mockMvc.perform(post("/teacher/availability").with(csrf()).param("action", "remove:1")
                        .param("items[0].dayOfWeek", "MONDAY").param("items[0].startTime", "19:00")
                        .param("items[0].endTime", "20:00").param("items[0].timezone", "Asia/Ho_Chi_Minh").param("items[0].active", "true")
                        .param("items[1].dayOfWeek", "TUESDAY").param("items[1].startTime", "18:00")
                        .param("items[1].endTime", "19:00").param("items[1].timezone", "Asia/Ho_Chi_Minh").param("items[1].active", "true"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"MONDAY\" selected")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"items[1].dayOfWeek\""))));
    }

    @Test
    void saveReplacesFullScheduleAndRedirects() throws Exception {
        mockMvc.perform(post("/teacher/availability").with(csrf()).param("action", "save")
                        .param("items[0].dayOfWeek", "MONDAY").param("items[0].startTime", "19:00")
                        .param("items[0].endTime", "20:00").param("items[0].timezone", "Asia/Ho_Chi_Minh").param("items[0].active", "true")
                        .param("items[1].dayOfWeek", "TUESDAY").param("items[1].startTime", "18:00")
                        .param("items[1].endTime", "19:00").param("items[1].timezone", "").param("items[1].active", "false"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/availability"))
                .andExpect(flash().attribute("availabilityNotice", "Đã lưu lịch rảnh."));
        verify(availability).replaceAvailabilities(eq(teacherId), eq(new ReplaceAvailabilityRequest(List.of(
                new AvailabilityItem(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(20, 0), "Asia/Ho_Chi_Minh", true),
                new AvailabilityItem(DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(19, 0), "Asia/Ho_Chi_Minh", false)))));
    }

    @Test
    void invalidTimeAndMissingDayStayInFormWithFieldErrors() throws Exception {
        mockMvc.perform(post("/teacher/availability").with(csrf()).param("action", "save")
                        .param("items[0].dayOfWeek", "").param("items[0].startTime", "bad-time")
                        .param("items[0].endTime", "20:00").param("items[0].timezone", "Asia/Ho_Chi_Minh").param("items[0].active", "true"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-availability"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chọn ngày trong tuần.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập giờ theo định dạng HH:mm.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"alert\" tabindex=\"-1\" autofocus")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"bad-time\"")));
    }

    @Test
    void overlappingSlotsShowSafeBusinessErrorAndRetainValues() throws Exception {
        doThrow(new BusinessException(ErrorCode.AVAILABILITY_TIME_CONFLICT)).when(availability).replaceAvailabilities(eq(teacherId), any());
        mockMvc.perform(post("/teacher/availability").with(csrf()).param("action", "save")
                        .param("items[0].dayOfWeek", "MONDAY").param("items[0].startTime", "19:00")
                        .param("items[0].endTime", "20:00").param("items[0].timezone", "Asia/Ho_Chi_Minh").param("items[0].active", "true"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Các khung giờ đang hoạt động bị trùng nhau")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"alert\" tabindex=\"-1\" autofocus")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"19:00\"")));
    }

    @Test
    void loadFailureDisablesEditsAndShowsRetry() throws Exception {
        when(availability.getAvailabilities(teacherId)).thenThrow(new IllegalStateException("private error"));
        mockMvc.perform(get("/teacher/availability"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được lịch hiện tại")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private error"))));
    }

    private AvailabilityView slot(DayOfWeek day, String start, String end, boolean active) {
        return new AvailabilityView(slotId, day, LocalTime.parse(start), LocalTime.parse(end), "Asia/Ho_Chi_Minh", active);
    }
}
