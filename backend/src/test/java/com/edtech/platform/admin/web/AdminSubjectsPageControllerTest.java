package com.edtech.platform.admin.web;

import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import com.edtech.platform.subject.facade.dto.SubjectResolutionCommand;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
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

@WebMvcTest(AdminSubjectsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSubjectsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminApprovalService approvals;
    @MockBean private SubjectFacade subjects;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID proposalId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID activeSubjectId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersPendingProposalAndActiveSubjectChoicesAsNativeForms() throws Exception {
        when(approvals.subjectProposals(any())).thenReturn(page(proposal()));
        when(subjects.getActiveSubjects()).thenReturn(List.of(new SubjectSnapshot(activeSubjectId,
                "MATH_12", "Toán học 12", "HIGH_SCHOOL", true)));
        mockMvc.perform(get("/admin/subjects"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-subjects"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duyệt đề xuất môn học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vật lý ứng dụng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán học 12")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\" value=\"3\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void clampsInvalidPageAndSizeAndShowsEmptyState() throws Exception {
        when(approvals.subjectProposals(any())).thenReturn(new PageImpl<>(List.of()));
        when(subjects.getActiveSubjects()).thenReturn(List.of());
        mockMvc.perform(get("/admin/subjects").param("page", "-5").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có đề xuất đang chờ")));
        verify(approvals).subjectProposals(argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 10));
    }

    @Test
    void keepsProposalErrorAndActiveSubjectErrorIndependent() throws Exception {
        when(approvals.subjectProposals(any())).thenThrow(new IllegalStateException("proposal store details"));
        when(subjects.getActiveSubjects()).thenThrow(new IllegalStateException("subject store details"));
        mockMvc.perform(get("/admin/subjects"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được đề xuất môn học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh mục môn đang hoạt động")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("store details"))));
    }

    @Test
    void createSubjectNormalizesCodeAndUsesCurrentProposalVersion() throws Exception {
        mockMvc.perform(post("/admin/subjects/{id}/create", proposalId)
                        .param("code", " math_12 ").param("name", " Toán học 12 ")
                        .param("educationLevel", "HIGH_SCHOOL").param("description", "Mô tả")
                        .param("note", "Đã kiểm tra").param("version", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subjects?page=0&size=10"));
        verify(approvals).approveSubject(eq(proposalId), any(UUID.class), argThat(command ->
                command.resolution() == SubjectResolutionCommand.Resolution.CREATE_NEW
                        && command.code().equals("MATH_12") && command.name().equals("Toán học 12")
                        && command.version() == 3L), any());
    }

    @Test
    void linkExistingSubjectUsesOnlyActiveCatalogChoice() throws Exception {
        mockMvc.perform(post("/admin/subjects/{id}/link", proposalId)
                        .param("existingSubjectId", activeSubjectId.toString()).param("note", "Liên kết đã có")
                        .param("version", "3").param("page", "1").param("size", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subjects?page=1&size=20"));
        verify(approvals).approveSubject(eq(proposalId), any(UUID.class), argThat(command ->
                command.resolution() == SubjectResolutionCommand.Resolution.LINK_EXISTING
                        && activeSubjectId.equals(command.existingSubjectId()) && command.version() == 3L), any());
    }

    @Test
    void invalidCreateFieldsRedirectWithoutCallingApprovalCommand() throws Exception {
        mockMvc.perform(post("/admin/subjects/{id}/create", proposalId)
                        .param("code", "bad code!").param("name", "Tên").param("educationLevel", "HIGH_SCHOOL")
                        .param("version", "3"))
                .andExpect(status().is3xxRedirection());
        verify(approvals, never()).approveSubject(any(), any(), any(), any());
    }

    @Test
    void createConflictKeepsEnteredValuesAndReopensTheFormAfterRedirect() throws Exception {
        when(approvals.approveSubject(eq(proposalId), any(UUID.class), any(SubjectResolutionCommand.class), any()))
                .thenThrow(new IllegalStateException("stale proposal"));
        mockMvc.perform(post("/admin/subjects/{id}/create", proposalId)
                        .param("code", "MATH_12").param("name", "Toán nâng cao")
                        .param("educationLevel", "HIGH_SCHOOL").param("description", "Mô tả mới")
                        .param("note", "Đã rà soát").param("version", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("createProposalId", proposalId.toString()))
                .andExpect(flash().attribute("createForm", new AdminSubjectsPageController.CreateFormValues(
                        "MATH_12", "Toán nâng cao", "HIGH_SCHOOL", "Mô tả mới", "Đã rà soát")));

        when(approvals.subjectProposals(any())).thenReturn(page(proposal()));
        when(subjects.getActiveSubjects()).thenReturn(List.of());
        mockMvc.perform(get("/admin/subjects")
                        .flashAttr("createProposalId", proposalId.toString())
                        .flashAttr("createForm", new AdminSubjectsPageController.CreateFormValues(
                                "MATH_12", "Toán nâng cao", "HIGH_SCHOOL", "Mô tả mới", "Đã rà soát")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<details class=\"admin-subjects__action\" open=\"open\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Toán nâng cao\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mô tả mới")));
    }

    @Test
    void rejectRequiresReasonAndKeepsItForCorrection() throws Exception {
        mockMvc.perform(post("/admin/subjects/{id}/reject", proposalId)
                        .param("reason", " ").param("version", "3"))
                .andExpect(status().is3xxRedirection());
        verify(approvals, never()).rejectSubject(any(), any(), anyString(), anyLong(), any());
    }

    @Test
    void rejectUsesVersionedAdminApprovalService() throws Exception {
        mockMvc.perform(post("/admin/subjects/{id}/reject", proposalId)
                        .param("reason", "Trùng với môn đang có").param("version", "3"))
                .andExpect(status().is3xxRedirection());
        verify(approvals).rejectSubject(eq(proposalId), any(UUID.class), eq("Trùng với môn đang có"), eq(3L), any());
    }

    private SubjectProposalSnapshot proposal() {
        return new SubjectProposalSnapshot(proposalId, teacherId, "Vật lý ứng dụng", "HIGH_SCHOOL",
                "Điện học và ứng dụng", "PENDING", null, null, null, null, 3L);
    }

    private PageImpl<SubjectProposalSnapshot> page(SubjectProposalSnapshot item) {
        return new PageImpl<>(List.of(item), PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt", "id")), 1);
    }
}
