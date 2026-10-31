package com.edtech.platform.subject.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.service.SubjectProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(TeacherSubjectProposalsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherSubjectProposalsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private SubjectProposalService proposals;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID proposalId = UUID.randomUUID();
    private final UUID createdSubjectId = UUID.randomUUID();
    private final SubjectProposalView rejected = new SubjectProposalView(proposalId, "Vật lý ứng dụng",
            EducationLevel.HIGH_SCHOOL, "Thực hành điện và năng lượng.", ProposalStatus.REJECTED,
            "Vui lòng mô tả rõ hơn phạm vi môn học.", Instant.parse("2026-10-28T12:00:00Z"), null, 0L);

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(proposals.getProposals(eq(teacherId), isNull(ProposalStatus.class), any()))
                .thenReturn(new PageImpl<>(List.of(rejected)));
    }

    @Test
    void rendersHistoryWithTranslatedStatusAndReviewFeedback() throws Exception {
        mockMvc.perform(get("/teacher/subject-proposals"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-subject-proposals"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vật lý ứng dụng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trung học phổ thông")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cần chỉnh sửa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng mô tả rõ hơn phạm vi môn học.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xét duyệt lúc 19:00, 28/10/2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"ELEMENTARY\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"PRIMARY_SCHOOL\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void renderedCreateFormCarriesCsrfToken() throws Exception {
        String html = mockMvc.perform(get("/teacher/subject-proposals").with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int actionIndex = html.lastIndexOf("action=\"/teacher/subject-proposals\"");
        int formStart = html.lastIndexOf("<form", actionIndex);
        int formEnd = html.indexOf("</form>", actionIndex);

        assertTrue(actionIndex >= 0 && formStart >= 0 && formEnd > actionIndex,
                "create subject proposal form should be present in the rendered page");
        assertTrue(html.substring(formStart, formEnd).contains("name=\"_csrf\""),
                "create subject proposal form should include its CSRF field");
    }

    @Test
    void statusFilterAndPaginationKeepQueryParameters() throws Exception {
        SubjectProposalView approved = new SubjectProposalView(UUID.randomUUID(), "Đại số nâng cao",
                EducationLevel.UNIVERSITY, null, ProposalStatus.APPROVED, null,
                Instant.parse("2026-10-27T12:00:00Z"), createdSubjectId, 1L);
        when(proposals.getProposals(eq(teacherId), eq(ProposalStatus.APPROVED), any()))
                .thenReturn(new PageImpl<>(List.of(approved), PageRequest.of(1, 10), 21));
        mockMvc.perform(get("/teacher/subject-proposals").param("status", "approved").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Môn đã được thêm vào danh mục.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status=APPROVED&amp;page=0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status=APPROVED&amp;page=2")));
        verify(proposals).getProposals(eq(teacherId), eq(ProposalStatus.APPROVED), any());
    }

    @Test
    void invalidStatusFallsBackToAllAndShowsNotice() throws Exception {
        mockMvc.perform(get("/teacher/subject-proposals").param("status", "invalid"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("đang hiển thị tất cả đề xuất")));
        verify(proposals).getProposals(eq(teacherId), isNull(ProposalStatus.class), any());
    }

    @Test
    void emptyHistoryShowsEmptyState() throws Exception {
        when(proposals.getProposals(eq(teacherId), isNull(ProposalStatus.class), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/teacher/subject-proposals"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có đề xuất nào")));
    }

    @Test
    void serviceFailureShowsRetryWithoutLeakingException() throws Exception {
        when(proposals.getProposals(eq(teacherId), isNull(ProposalStatus.class), any()))
                .thenThrow(new IllegalStateException("private diagnostic"));
        mockMvc.perform(get("/teacher/subject-proposals"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được đề xuất")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private diagnostic"))));
    }

    @Test
    void createUsesValidatedRequestAndRedirectsAfterSuccess() throws Exception {
        mockMvc.perform(post("/teacher/subject-proposals").with(csrf())
                        .param("proposedName", "  Vật lý ứng dụng  ")
                        .param("educationLevel", "HIGH_SCHOOL")
                        .param("description", "  Môn thực hành  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/subject-proposals?status=ALL&page=0"))
                .andExpect(flash().attribute("proposalNotice", "Đã gửi đề xuất môn học để xét duyệt."));
        verify(proposals).createProposal(teacherId,
                new CreateSubjectProposalRequest("Vật lý ứng dụng", EducationLevel.HIGH_SCHOOL, "Môn thực hành"));
    }

    @Test
    void invalidFormPreservesValuesAndFocusesFirstInvalidField() throws Exception {
        mockMvc.perform(post("/teacher/subject-proposals").with(csrf())
                        .param("proposedName", "<script>alert(1)</script>")
                        .param("educationLevel", "HIGH_SCHOOL"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-subject-proposals"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tên môn không được chứa ký tự &lt;.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("autofocus=\"autofocus\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"proposedName\"")));
    }

    @Test
    void invalidDescriptionFocusesItsField() throws Exception {
        mockMvc.perform(post("/teacher/subject-proposals").with(csrf())
                        .param("proposedName", "Môn mới")
                        .param("educationLevel", "OTHER")
                        .param("description", "<b>không hợp lệ</b>"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mô tả không được chứa ký tự &lt;.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"description\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("autofocus=\"autofocus\"")));
    }

    @Test
    void serviceErrorPreservesProposalAndShowsSafeMessage() throws Exception {
        when(proposals.createProposal(teacherId,
                new CreateSubjectProposalRequest("Môn mới", EducationLevel.OTHER, null)))
                .thenThrow(new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        mockMvc.perform(post("/teacher/subject-proposals").with(csrf())
                        .param("proposedName", "Môn mới").param("educationLevel", "OTHER"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tìm thấy hồ sơ gia sư của bạn.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"M&ocirc;n mới\"")));
    }
}
