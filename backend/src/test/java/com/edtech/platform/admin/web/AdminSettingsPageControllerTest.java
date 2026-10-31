package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminSettingsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSettingsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminSettingsService settings;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersCurrentSettingsAndNoStore() throws Exception {
        when(settings.getSettings()).thenReturn(settingsView());
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-settings"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"commissionRate\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("5.00")));
    }

    @Test
    void rendersSafeErrorWhenSettingsCannotLoad() throws Exception {
        when(settings.getSettings()).thenThrow(new IllegalStateException("private database message"));
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được cài đặt nền tảng")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private database message"))));
    }

    @Test
    void updateUsesValidatedDtoAndAdminActor() throws Exception {
        mockMvc.perform(post("/admin/settings").param("commissionRate", "7.50")
                        .param("bayesianMinimumReviews", "8").param("bookingReminderHours", "24")
                        .param("bookingExpirationHours", "36"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));
        verify(settings).updateSettings(eq(adminId), eq(new UpdatePlatformSettingsRequest(new BigDecimal("7.50"), 8, 24, 36)));
    }

    @Test
    void validationFailureDoesNotCallServiceAndUsesPrg() throws Exception {
        mockMvc.perform(post("/admin/settings").param("commissionRate", "100.01")
                        .param("bayesianMinimumReviews", "-1").param("bookingReminderHours", "0")
                        .param("bookingExpirationHours", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));
        verify(settings, never()).updateSettings(any(UUID.class), any(UpdatePlatformSettingsRequest.class));
    }

    @Test
    void malformedNumberReturnsToSettingsWithoutLeakingException() throws Exception {
        mockMvc.perform(post("/admin/settings").param("commissionRate", "oops")
                        .param("bayesianMinimumReviews", "8").param("bookingReminderHours", "24")
                        .param("bookingExpirationHours", "36"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));
        verify(settings, never()).updateSettings(any(UUID.class), any(UpdatePlatformSettingsRequest.class));
    }

    private PlatformSettingsView settingsView() {
        return new PlatformSettingsView(UUID.randomUUID(), new BigDecimal("5.00"), 10, 11, 12,
                Instant.parse("2026-10-28T12:00:00Z"));
    }
}
