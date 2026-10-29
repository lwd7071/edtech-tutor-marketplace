package com.edtech.platform.admin.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.dto.AdminCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCredentialsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCredentialsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherCredentialService credentials;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID credentialId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersPrivateCredentialDetailsWithoutLeakingRestProofUrlOrJavaScript() throws Exception {
        when(credentials.adminList("PENDING")).thenReturn(List.of(row(CredentialStatus.PENDING)));
        mockMvc.perform(get("/admin/credentials"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-credentials"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bằng cử nhân Sư phạm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/credentials/" + credentialId + "/proof")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/api/admin/credentials"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\" value=\"7\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void statusLinksUseOnlyContractStatusesAndInvalidStatusFallsBackToPending() throws Exception {
        when(credentials.adminList("PENDING")).thenReturn(List.of());
        mockMvc.perform(get("/admin/credentials").param("status", "UNSUPPORTED"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có minh chứng ở trạng thái này")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status=PENDING")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status=APPROVED")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status=REJECTED")));
        verify(credentials).adminList("PENDING");
    }

    @Test
    void showsRejectionReasonForReviewedCredential() throws Exception {
        when(credentials.adminList("REJECTED")).thenReturn(List.of(row(CredentialStatus.REJECTED)));
        mockMvc.perform(get("/admin/credentials").param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tệp chưa đọc được")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Xác nhận từ chối"))));
    }

    @Test
    void queueFailureDoesNotExposeInternalDetails() throws Exception {
        when(credentials.adminList("PENDING")).thenThrow(new IllegalStateException("credential repository secret"));
        mockMvc.perform(get("/admin/credentials"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách minh chứng")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("credential repository secret"))));
    }

    @Test
    void approveCarriesCurrentVersionAndUsesPostRedirectGet() throws Exception {
        mockMvc.perform(post("/admin/credentials/{id}/approve", credentialId)
                        .param("version", "7").param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/credentials?status=PENDING"));
        verify(credentials).approve(eq(credentialId), any(UUID.class), eq(7L));
    }

    @Test
    void rejectRequiresReasonAndPreservesItAfterRedirect() throws Exception {
        mockMvc.perform(post("/admin/credentials/{id}/reject", credentialId)
                        .param("reason", " ").param("version", "7").param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/credentials?status=PENDING"));
        verify(credentials, never()).reject(any(), anyString(), any(), any());
    }

    @Test
    void proofRouteStreamsSameOriginPrivateAttachmentWithSafeHeaders() throws Exception {
        when(credentials.adminProof(credentialId)).thenReturn(new CredentialProofStorage.DownloadedFile(new byte[]{4, 5}, "application/pdf"));
        mockMvc.perform(get("/admin/credentials/{id}/proof", credentialId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=credential-proof"))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(new byte[]{4, 5}));
    }

    private AdminCredentialView row(CredentialStatus status) {
        return new AdminCredentialView(credentialId, teacherId, "Bằng cử nhân Sư phạm",
                "/api/admin/credentials/" + credentialId + "/proof", status,
                status == CredentialStatus.REJECTED ? "Tệp chưa đọc được" : null,
                Instant.parse("2026-10-28T10:00:00Z"), 7);
    }
}
