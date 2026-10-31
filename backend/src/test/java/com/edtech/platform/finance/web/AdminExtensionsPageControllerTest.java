package com.edtech.platform.finance.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.finance.command.ApproveExtensionCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
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
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminExtensionsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminExtensionsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ExtensionService extensions;
    @MockBean private FinanceCommandExecutor commands;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private java.time.Clock clock;

    private final UUID adminId = UUID.randomUUID();
    private final UUID extensionId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersPendingExtensionQueueWithRequestedExpiryAndNativeActions() throws Exception {
        when(extensions.findAdminExtensions(eq("PENDING"), any())).thenReturn(new PageImpl<>(List.of(request())));
        mockMvc.perform(get("/admin/extensions").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duyệt gia hạn gói học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2027-01-05T12:00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/extensions/" + extensionId + "/approve")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void normalizesFilterAndPagesNewestFirst() throws Exception {
        when(extensions.findAdminExtensions(eq((String) null), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/extensions").param("status", "invalid").param("page", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có yêu cầu ở trạng thái này")));
        verify(extensions).findAdminExtensions(eq((String) null), argThat(page -> page.getPageNumber() == 0
                && page.getPageSize() == 20 && page.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
                && page.getSort().getOrderFor("id").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void showsSafeUnavailableState() throws Exception {
        when(extensions.findAdminExtensions(anyString(), any())).thenThrow(new IllegalStateException("storage-details"));
        mockMvc.perform(get("/admin/extensions"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách yêu cầu gia hạn")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("storage-details"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void approveUsesRequestedDateConvertedFromVietnameseLocalTime() throws Exception {
        String longAdminNote = "Đã kiểm tra. ".repeat(100);
        doAnswer(invocation -> ((Supplier<ApiResponse<ExtensionRequestView>>) invocation.getArgument(5)).get())
                .when(commands).execute(eq(adminId), eq("ADMIN_EXTENSION_APPROVE"), eq(key), any(),
                        eq(ExtensionRequestView.class), any());
        when(extensions.approveExtension(eq(adminId), eq(extensionId), any())).thenReturn(request());
        mockMvc.perform(post("/admin/extensions/{id}/approve", extensionId)
                        .param("approvedExpiryDate", "2027-01-06T15:00").param("adminNote", " " + longAdminNote + " ")
                        .param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/extensions?status=ALL&page=0"));
        verify(extensions).approveExtension(eq(adminId), eq(extensionId), eq(new ApproveExtensionCommand(
                Instant.parse("2027-01-06T08:00:00Z"), longAdminNote.trim())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectCallsServiceThroughIdempotencyExecutor() throws Exception {
        doAnswer(invocation -> ((Supplier<ApiResponse<ExtensionRequestView>>) invocation.getArgument(5)).get())
                .when(commands).execute(eq(adminId), eq("ADMIN_EXTENSION_REJECT"), eq(key), any(),
                        eq(ExtensionRequestView.class), any());
        when(extensions.rejectExtension(eq(adminId), eq(extensionId), any())).thenReturn(request());
        mockMvc.perform(post("/admin/extensions/{id}/reject", extensionId)
                        .param("reason", "Gói học không đáp ứng điều kiện gia hạn")
                        .param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection());
        verify(extensions).rejectExtension(eq(adminId), eq(extensionId), eq(new RejectFinanceCommand(
                "Gói học không đáp ứng điều kiện gia hạn", 0)));
    }

    private ExtensionRequestView request() {
        return new ExtensionRequestView(extensionId, UUID.randomUUID(), UUID.randomUUID(), "Lý do gia hạn",
                Instant.parse("2027-01-05T05:00:00Z"), null, ExtensionStatus.PENDING,
                null, null, null, Instant.parse("2026-10-28T12:00:00Z"));
    }
}
