package com.edtech.platform.admin.web;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.dto.response.AuditLogView;
import com.edtech.platform.admin.service.AuditLogQueryService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminAuditLogsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuditLogsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private AuditLogQueryService auditLogs;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID adminId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersAuditDataEscapedAndNoStore() throws Exception {
        when(auditLogs.findAuditLogs(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(auditLog())));
        mockMvc.perform(get("/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-audit-logs"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khóa người dùng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert"))));
    }

    @Test
    void appliesFiltersAndStablePageSize() throws Exception {
        when(auditLogs.findAuditLogs(eq(adminId), eq(AuditAction.USER_LOCKED), eq("USER"), eq(targetId), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/audit-logs").param("actorId", adminId.toString())
                        .param("action", "USER_LOCKED").param("targetType", " USER ")
                        .param("targetId", targetId.toString()).param("page", "-2"))
                .andExpect(status().isOk());
        verify(auditLogs).findAuditLogs(eq(adminId), eq(AuditAction.USER_LOCKED), eq("USER"), eq(targetId),
                argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20
                        && page.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void invalidUuidOrUnknownActionDoesNotCallQuery() throws Exception {
        mockMvc.perform(get("/admin/audit-logs").param("actorId", "not-a-uuid").param("action", "surprise"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập đúng mã người thực hiện dạng UUID")));
    }

    @Test
    void supportsEmptyAndUnavailableStates() throws Exception {
        when(auditLogs.findAuditLogs(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có bản ghi phù hợp")));
        when(auditLogs.findAuditLogs(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("internal query error"));
        mockMvc.perform(get("/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được nhật ký thao tác")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal query error"))));
    }

    private AuditLogView auditLog() {
        return new AuditLogView(UUID.randomUUID(), adminId, AuditAction.USER_LOCKED, "USER", targetId,
                Map.of("status", "ACTIVE"), Map.of("status", "LOCKED", "note", "<script>alert(1)</script>"),
                "127.0.0.1", "Example Browser", Instant.parse("2026-10-28T12:00:00Z"));
    }
}
