package com.edtech.platform.communication.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.communication.dto.notification.NotificationView;
import com.edtech.platform.communication.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NotificationPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private NotificationService notifications;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()));
    }

    @Test
    void rendersNotificationsAndOnlyLinksToImplementedOwnedDestinations() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var assignment = notification("Bài tập mới", "Gia sư đã giao bài.", "ASSIGNMENT", true);
        var booking = notification("Lịch học mới", "Một lịch học đã thay đổi.", "BOOKING", false);
        when(notifications.getNotifications(eq(studentId), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(assignment, booking), pageable, 2));

        mockMvc.perform(get("/student/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/notifications"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bài tập mới")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/assignments/" + assignment.getReferenceId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/bookings/" + booking.getReferenceId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đánh dấu đã đọc")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/student/requests/"))));
    }

    @Test
    void appliesUnreadAndCategoryFiltersThroughGetQuery() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(notifications.getNotifications(eq(studentId), eq(null), eq("ASSIGNMENT"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/student/notifications").param("filter", "assignment"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có thông báo ở mục này")));
    }

    @Test
    void rendersEmptyAndUnavailableStatesWithoutScripts() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(notifications.getNotifications(eq(studentId), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        mockMvc.perform(get("/student/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có thông báo ở mục này")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));

        when(notifications.getNotifications(eq(studentId), eq(null), eq(null), eq(pageable)))
                .thenThrow(new IllegalStateException("unavailable"));
        mockMvc.perform(get("/student/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được thông báo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")));
    }

    @Test
    void rendersTeacherCopyAndRoleSpecificFilters() throws Exception {
        UUID teacherId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(notifications.getNotifications(eq(teacherId), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/teacher/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gia sư")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lịch dạy")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Điều hướng học viên"))));
    }

    private NotificationView notification(String title, String content, String referenceType, boolean isRead) {
        UUID referenceId = UUID.randomUUID();
        return NotificationView.builder().id(UUID.randomUUID()).userId(studentId).type(referenceType)
                .title(title).content(content).referenceType(referenceType).referenceId(referenceId)
                .referenceUrl("/student/" + ("ASSIGNMENT".equals(referenceType) ? "assignments" : "bookings") + "/" + referenceId)
                .isRead(isRead).createdAt(Instant.parse("2026-10-28T08:00:00Z")).build();
    }
}
