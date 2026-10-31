package com.edtech.platform.learning.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.enrollment.service.TeacherLearnerService;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.AssignmentType;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateAssignmentRequest;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.learning.service.TeacherAssignmentService;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherAssignmentPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherAssignmentPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AssignmentReadService reads;
    @MockBean private TeacherAssignmentService commands;
    @MockBean private TeacherLearnerService learners;
    @MockBean private TeacherSubjectService teacherSubjects;
    @MockBean private AttachmentService attachmentService;
    @MockBean private AttachmentFacade attachmentFacade;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID submissionId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void listRendersAssignmentAndNativePagination() throws Exception {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(reads.teacherList(teacherId, pageable)).thenReturn(new PageImpl<>(List.of(row()), pageable, 1));
        mockMvc.perform(get("/teacher/assignments"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-assignments"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bài tập Đại số")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Minh An · Đại số")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bài nộp: Chờ chấm")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void createSavesNativeFormAsVersionZeroDraft() throws Exception {
        mockMvc.perform(post("/teacher/assignments")
                        .param("studentId", studentId.toString()).param("subjectId", subjectId.toString())
                        .param("title", "Bài tập Đại số").param("content", "Giải bài 1 và 2")
                        .param("action", "draft"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/assignments"));
        ArgumentCaptor<CreateAssignmentRequest> captor = ArgumentCaptor.forClass(CreateAssignmentRequest.class);
        verify(commands).createAssignment(eq(teacherId), captor.capture());
        assertThat(captor.getValue().getVersion()).isZero();
        assertThat(captor.getValue().getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(captor.getValue().getAssignmentType()).isEqualTo(AssignmentType.FREEFORM);
        assertThat(captor.getValue().getContentBlocks()).extracting(ContentBlock::getContent).containsExactly("Giải bài 1 và 2");
    }

    @Test
    void createFormPaginatesEligibleLearnersWithoutJavascript() throws Exception {
        var pageable = PageRequest.of(0, 50);
        when(learners.list(teacherId, null, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 51));
        when(teacherSubjects.getSubjects(teacherId)).thenReturn(List.of());
        mockMvc.perform(get("/teacher/assignments/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-assignment-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang danh sách học viên")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Học viên tiếp")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"learnerPage\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void detailRendersDraftActionsAndNoScriptEditForm() throws Exception {
        when(reads.detail(teacherId, assignmentId, true)).thenReturn(row());
        mockMvc.perform(get("/teacher/assignments/{id}", assignmentId))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-assignment-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chỉnh sửa bản nháp")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giao bài")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void publishUsesCurrentVersionAndRedirectsBack() throws Exception {
        mockMvc.perform(post("/teacher/assignments/{id}/publish", assignmentId).param("version", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/assignments/" + assignmentId));
        verify(commands).transition(teacherId, assignmentId, AssignmentStatus.PUBLISHED, 4L);
    }

    @Test
    void submissionShowsVersionedGradeForm() throws Exception {
        SubmissionDetail submission = new SubmissionDetail(submissionId, assignmentId, studentId, List.of(
                new ContentBlock("TEXT", "Lời giải", null, null, null)), now, SubmissionStatus.SUBMITTED,
                null, null, null, 2L);
        when(reads.submission(teacherId, submissionId)).thenReturn(submission);
        when(reads.detail(teacherId, assignmentId, true)).thenReturn(row());
        mockMvc.perform(get("/teacher/submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-submission-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lời giải")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"score\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\" value=\"2\"")));
    }

    @Test
    void gradeUsesVersionedScoreAndFeedback() throws Exception {
        mockMvc.perform(post("/teacher/submissions/{id}/grade", submissionId)
                        .param("version", "2").param("score", "8.5").param("feedbackText", "Lập luận rõ."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/submissions/" + submissionId));
        ArgumentCaptor<GradeSubmissionRequest> captor = ArgumentCaptor.forClass(GradeSubmissionRequest.class);
        verify(commands).gradeSubmission(eq(teacherId), eq(submissionId), captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(2L);
        assertThat(captor.getValue().getScore()).isEqualByComparingTo("8.5");
    }

    private Map<String, Object> row() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", assignmentId);
        map.put("teacherId", teacherId);
        map.put("studentId", studentId);
        map.put("subjectId", subjectId);
        map.put("title", "Bài tập Đại số");
        map.put("assignmentType", AssignmentType.FREEFORM);
        map.put("contentBlocks", List.of(new ContentBlock("TEXT", "Giải phương trình", null, null, null)));
        map.put("dueAt", now.plusSeconds(3600));
        map.put("status", AssignmentStatus.DRAFT);
        map.put("version", 4L);
        map.put("studentName", "Minh An");
        map.put("subjectName", "Đại số");
        map.put("submissions", List.of(new SubmissionDetail(submissionId, assignmentId, studentId, List.of(), now,
                SubmissionStatus.SUBMITTED, null, null, null, 2L)));
        return map;
    }
}
