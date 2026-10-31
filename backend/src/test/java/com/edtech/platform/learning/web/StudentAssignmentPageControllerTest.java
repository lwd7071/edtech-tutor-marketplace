package com.edtech.platform.learning.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.AssignmentType;
import com.edtech.platform.learning.domain.StudentAssignmentProgress;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.StudentAssignmentDetail;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.StudentAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentAssignmentPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentAssignmentPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private StudentAssignmentService assignments;
    @MockBean private AttachmentService attachments;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID attachmentId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-28T09:00:00Z");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.com", "STUDENT"), null, List.of()));
        when(clock.instant()).thenReturn(now);
    }

    @Test
    void listUsesNativeGetProgressFilterAndPagination() throws Exception {
        var assignment = assignment(AssignmentStatus.PUBLISHED, now.plusSeconds(3600));
        var pageable = PageRequest.of(1, 10);
        when(assignments.getAssignments(eq(studentId), isNull(), eq(StudentAssignmentProgress.SUBMITTED), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(assignment), pageable, 12));

        mockMvc.perform(get("/student/assignments").param("progress", "SUBMITTED").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/assignments"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Câu hỏi đại số dài")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang 2 / 2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã nộp")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"progress\"")));
    }

    @Test
    void invalidProgressFallsBackToTodoAndExplainsFilter() throws Exception {
        var pageable = PageRequest.of(0, 10);
        when(assignments.getAssignments(eq(studentId), isNull(), eq(StudentAssignmentProgress.TODO), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/student/assignments").param("progress", "not-a-status"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tiến độ lọc không hợp lệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có bài tập ở mục này")));
    }

    @Test
    void detailRendersPromptAndTextSubmissionFormWithoutJavascript() throws Exception {
        when(assignments.getDetail(studentId, assignmentId)).thenReturn(detail(AssignmentStatus.PUBLISHED, null));

        mockMvc.perform(get("/student/assignments/{id}", assignmentId))
                .andExpect(status().isOk())
                .andExpect(view().name("student/assignment-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đề bài mẫu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void unknownOrUnownedAssignmentReturnsNotFound() throws Exception {
        when(assignments.getDetail(studentId, assignmentId)).thenThrow(new com.edtech.platform.common.exception.BusinessException(
                com.edtech.platform.common.exception.ErrorCode.ASSIGNMENT_NOT_FOUND));

        mockMvc.perform(get("/student/assignments/{id}", assignmentId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("student/assignment-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được bài tập")));
    }

    @Test
    void systemQuizIsNotPresentedAsFreeformSubmission() throws Exception {
        var quiz = new AssignmentDetail(assignmentId, UUID.randomUUID(), studentId, UUID.randomUUID(),
                "Trắc nghiệm", AssignmentType.SYSTEM_QUIZ, List.of(), null, now.plusSeconds(3600),
                AssignmentStatus.PUBLISHED, 1L);
        when(assignments.getDetail(studentId, assignmentId)).thenReturn(new StudentAssignmentDetail(quiz, null, List.of(), List.of()));

        mockMvc.perform(get("/student/assignments/{id}", assignmentId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chưa có giao diện làm bài đã được xác minh")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"status\""))));
    }

    @Test
    void nativeMultipartPostUploadsFileAndUsesVersionedSubmissionCommand() throws Exception {
        var existing = detail(AssignmentStatus.PUBLISHED, null);
        when(assignments.getDetail(studentId, assignmentId)).thenReturn(existing);
        when(attachments.uploadAttachment(eq(studentId), eq(com.edtech.platform.common.domain.AttachableType.SUBMISSION), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AttachmentView(attachmentId, "https://files.example/answer.pdf", "answer.pdf", "application/pdf", 120L));
        when(assignments.createOrUpdateSubmission(eq(studentId), eq(assignmentId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SubmissionDetail(UUID.randomUUID(), assignmentId, studentId, List.of(), now,
                        SubmissionStatus.SUBMITTED, null, null, null, 1L));

        mockMvc.perform(multipart("/student/assignments/{id}/submission", assignmentId)
                        .file(new MockMultipartFile("files", "answer.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .param("version", "0").param("status", "SUBMITTED").param("answer", "Câu trả lời"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/assignments/" + assignmentId));

        ArgumentCaptor<CreateSubmissionRequest> request = ArgumentCaptor.forClass(CreateSubmissionRequest.class);
        verify(assignments).createOrUpdateSubmission(eq(studentId), eq(assignmentId), request.capture());
        assertThat(request.getValue().getVersion()).isZero();
        assertThat(request.getValue().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(request.getValue().getContentBlocks()).extracting(ContentBlock::getType)
                .containsExactly("TEXT", "FILE");
        assertThat(request.getValue().getContentBlocks().get(1).getAttachmentId()).isEqualTo(attachmentId);
    }

    private AssignmentDetail assignment(AssignmentStatus status, Instant dueAt) {
        return new AssignmentDetail(assignmentId, UUID.randomUUID(), studentId, UUID.randomUUID(),
                "Câu hỏi đại số dài", AssignmentType.FREEFORM,
                List.of(new ContentBlock("TEXT", "Đề bài mẫu", null, null, null)), null, dueAt, status, 2L);
    }

    private StudentAssignmentDetail detail(AssignmentStatus status, SubmissionDetail submission) {
        return new StudentAssignmentDetail(assignment(status, now.plusSeconds(3600)), submission, List.of(), List.of());
    }
}
