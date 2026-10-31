package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.response.AdminUserView;
import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AdminUserDirectoryService;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUsersPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUsersPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminUserDirectoryService directory;
    @MockBean private AdminApprovalService approvals;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID userId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(actorId, "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersUsersWithVietnameseTimeAndNativeModerationForm() throws Exception {
        when(directory.findUsers(isNull(), isNull(), isNull(), any())).thenReturn(page(user("ACTIVE")));
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-users"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lê Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29/10/2026 00:00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"status\" value=\"LOCKED\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void appliesUrlSearchFiltersSortAndPagination() throws Exception {
        when(directory.findUsers(eq("an"), eq("TEACHER"), eq("LOCKED"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 50), 60));
        mockMvc.perform(get("/admin/users").param("keyword", " an ").param("role", "TEACHER")
                        .param("status", "LOCKED").param("page", "1").param("size", "50")
                        .param("sortField", "email").param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"an\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sort=email,asc")));
        verify(directory).findUsers(eq("an"), eq("TEACHER"), eq("LOCKED"), argThat(page ->
                page.getPageNumber() == 1 && page.getPageSize() == 50
                        && page.getSort().getOrderFor("email").getDirection() == Sort.Direction.ASC
                        && page.getSort().getOrderFor("id") != null));
    }

    @Test
    void rejectsLongKeywordWithoutCallingDirectoryAndShowsRecovery() throws Exception {
        mockMvc.perform(get("/admin/users").param("keyword", "x".repeat(101)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Từ khóa tối đa 100 ký tự")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rút ngắn từ khóa")));
        verifyNoInteractions(directory);
    }

    @Test
    void normalizesInvalidRoleStatusAndSortWithoutFailingThePage() throws Exception {
        when(directory.findUsers(isNull(), isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/users").param("role", "ADMIN").param("status", "PENDING")
                        .param("sortField", "password").param("sortDirection", "sideways"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Một số bộ lọc hoặc cách sắp xếp không hợp lệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"createdAt\" selected=\"selected\"")));
    }

    @Test
    void keepsEmptyAndServiceErrorStatesSeparate() throws Exception {
        when(directory.findUsers(isNull(), isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy người dùng")));
        reset(directory);
        when(directory.findUsers(isNull(), isNull(), isNull(), any())).thenThrow(new IllegalStateException("private database detail"));
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách người dùng")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private database detail"))));
    }

    @Test
    void clampsPageAndSizeAndRedirectsWhenRequestedPageIsOutOfRange() throws Exception {
        when(directory.findUsers(isNull(), isNull(), isNull(), any())).thenReturn(
                new PageImpl<>(List.of(user("ACTIVE")), PageRequest.of(0, 20), 21));
        mockMvc.perform(get("/admin/users").param("page", "8").param("size", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=1&size=20&sort=createdAt,desc"));
        verify(directory).findUsers(isNull(), isNull(), isNull(), argThat(page -> page.getPageNumber() == 8 && page.getPageSize() == 20));
    }

    @Test
    void changesUserStatusWithReasonAndAdminAuditContext() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/status", userId).param("status", "LOCKED")
                        .param("reason", "Vi phạm quy định").param("keyword", "an").param("role", "STUDENT")
                        .param("statusFilter", "ACTIVE").param("page", "2").param("size", "50")
                        .param("sort", "email,asc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=2&size=50&sort=email,asc&keyword=an&role=STUDENT&status=ACTIVE"));
        verify(approvals).changeUserStatus(eq(userId), eq(actorId), eq("LOCKED"), eq("Vi phạm quy định"), any());
    }

    @Test
    void blankOrTooLongReasonDoesNotCallModerationService() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/status", userId).param("status", "LOCKED").param("reason", " "))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/{id}/status", userId).param("status", "LOCKED").param("reason", "x".repeat(1001)))
                .andExpect(status().is3xxRedirection());
        verify(approvals, never()).changeUserStatus(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void moderationFailureRetainsReasonAndReopensTheNativeForm() throws Exception {
        when(approvals.changeUserStatus(eq(userId), eq(actorId), eq("LOCKED"), eq("Duplicate account"), any()))
                .thenThrow(new IllegalStateException("business conflict"));
        mockMvc.perform(post("/admin/users/{id}/status", userId)
                        .param("status", "LOCKED").param("reason", "Duplicate account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("moderationUserId", userId.toString()))
                .andExpect(flash().attribute("moderationReason", "Duplicate account"));
        when(directory.findUsers(isNull(), isNull(), isNull(), any())).thenReturn(page(user("ACTIVE")));
        mockMvc.perform(get("/admin/users").flashAttr("moderationUserId", userId.toString())
                        .flashAttr("moderationReason", "Duplicate account"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<details class=\"admin-users__moderation\" open=\"open\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duplicate account")));
    }

    private AdminUserView user(String status) {
        return new AdminUserView(userId, "Lê Minh An", "an@example.test", "STUDENT", status,
                Instant.parse("2026-10-28T17:00:00Z"), null);
    }

    private PageImpl<AdminUserView> page(AdminUserView item) {
        return new PageImpl<>(List.of(item), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")), 1);
    }
}
