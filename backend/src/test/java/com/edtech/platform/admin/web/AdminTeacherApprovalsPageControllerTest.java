package com.edtech.platform.admin.web;

import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import com.edtech.platform.teacher.facade.dto.TeacherDocumentSnapshot;
import com.edtech.platform.teacher.service.TeacherDocumentService;
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

import java.util.List;
import java.util.UUID;
import java.time.Clock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTeacherApprovalsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTeacherApprovalsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminApprovalService approvals;
    @MockBean private TeacherDocumentService documents;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID profileId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersApprovalQueueAndPrivateProofLinkWithoutJavaScript() throws Exception {
        var snapshot = new TeacherApprovalSnapshot(profileId, UUID.randomUUID(), "PENDING_APPROVAL", null, null, null,
                List.of(new TeacherDocumentSnapshot(documentId, "IDENTITY", "CCCD", "image/png", 123L, "PENDING")),
                "Nguyễn Thị Gia Sư", "teacher@example.test", "Giới thiệu đầy đủ", 4,
                List.of("Tiếng Việt", "English"), true, false, "Quận 1", "https://video.example.test/intro");
        when(approvals.teacherApprovals(eq("PENDING_APPROVAL"), any())).thenReturn(new PageImpl<>(List.of(snapshot),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt", "id")), 1));

        mockMvc.perform(get("/admin/teachers"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-teachers"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Thị Gia Sư")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CCCD")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/teachers/documents/" + documentId + "/proof")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://video.example.test/intro")));
    }

    @Test
    void invalidStatusAndPageSizeFallBackToSafeValuesAndEmptyQueueHasExplicitState() throws Exception {
        when(approvals.teacherApprovals(eq("PENDING_APPROVAL"), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/teachers").param("status", "UNKNOWN").param("page", "-9").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có hồ sơ ở trạng thái này")));
        verify(approvals).teacherApprovals(eq("PENDING_APPROVAL"), argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20));
    }

    @Test
    void serviceFailureIsGenericAndDoesNotLeakInternalMessage() throws Exception {
        when(approvals.teacherApprovals(anyString(), any())).thenThrow(new IllegalStateException("private database detail"));
        mockMvc.perform(get("/admin/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách hồ sơ")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private database detail"))));
    }

    @Test
    void approveUsesDomainServiceAndPostRedirectGet() throws Exception {
        mockMvc.perform(post("/admin/teachers/{id}/approve", profileId)
                        .param("note", "Đủ giấy tờ").param("status", "PENDING_APPROVAL").param("page", "2").param("size", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/teachers?status=PENDING_APPROVAL&page=2&size=10"));
        verify(approvals).approveTeacher(eq(profileId), any(UUID.class), eq("Đủ giấy tờ"), any());
    }

    @Test
    void rejectRequiresReasonAndPreservesItForCorrection() throws Exception {
        mockMvc.perform(post("/admin/teachers/{id}/reject", profileId)
                        .param("reason", "   ").param("status", "PENDING_APPROVAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/teachers?status=PENDING_APPROVAL&page=0&size=20"));
        verify(approvals, never()).rejectTeacher(any(), any(), anyString(), any());
    }

    @Test
    void proofUsesAdminServiceWithNoStoreAndNosniffHeaders() throws Exception {
        when(documents.adminProof(documentId)).thenReturn(new CredentialProofStorage.DownloadedFile(new byte[]{1, 2}, "image/png"));
        mockMvc.perform(get("/admin/teachers/documents/{id}/proof", documentId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=teacher-proof"))
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(new byte[]{1, 2}));
    }
}
