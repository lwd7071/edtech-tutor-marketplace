package com.edtech.platform.teacher.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(TeacherSubjectsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherSubjectsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherSubjectService teacherSubjects;
    @MockBean private SubjectFacade subjects;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID assignedId = UUID.randomUUID();
    private final UUID availableId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(teacherSubjects.getSubjects(teacherId)).thenReturn(List.of(new TeacherSubjectView(
                UUID.randomUUID(), new TeacherSubjectView.SubjectDto(assignedId, "MATH", "Toán học", "HIGH_SCHOOL"),
                "Lớp 6 đến 12", "Năm năm luyện thi", true)));
        when(subjects.getActiveSubjects()).thenReturn(List.of(
                new SubjectSnapshot(assignedId, "MATH", "Toán học", "HIGH_SCHOOL", true),
                new SubjectSnapshot(availableId, "PHYSICS", "Vật lý", "HIGH_SCHOOL", true)));
    }

    @Test
    void rendersAssignedSubjectsAndOnlyUnassignedCatalogOptions() throws Exception {
        mockMvc.perform(get("/teacher/subjects"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-subjects"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Môn đang có trong hồ sơ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trung học phổ thông")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("HIGH_SCHOOL"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Năm năm luyện thi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"subjectId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(availableId.toString())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"" + assignedId + "\""))));
    }

    @Test
    void renderedAssignFormCarriesCsrfToken() throws Exception {
        String html = mockMvc.perform(get("/teacher/subjects").with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int actionIndex = html.indexOf("action=\"/teacher/subjects/assign\"");
        int formStart = html.lastIndexOf("<form", actionIndex);
        int formEnd = html.indexOf("</form>", actionIndex);

        assertTrue(actionIndex >= 0 && formStart >= 0 && formEnd > actionIndex,
                "assign subject form should be present in the rendered page");
        assertTrue(html.substring(formStart, formEnd).contains("name=\"_csrf\""),
                "assign subject form should include its CSRF field");
    }

    @Test
    void showsEmptyStateWhenTeacherHasNoSubjects() throws Exception {
        when(teacherSubjects.getSubjects(teacherId)).thenReturn(List.of());
        mockMvc.perform(get("/teacher/subjects"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có môn dạy")));
    }

    @Test
    void listFailureShowsRetryAndDoesNotOfferMutation() throws Exception {
        when(teacherSubjects.getSubjects(teacherId)).thenThrow(new IllegalStateException("private detail"));
        mockMvc.perform(get("/teacher/subjects"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách môn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tải lại danh sách")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private detail"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")));
    }

    @Test
    void catalogFailureKeepsAssignedListVisibleAndDisablesAdd() throws Exception {
        when(subjects.getActiveSubjects()).thenThrow(new IllegalStateException("catalog unavailable"));
        mockMvc.perform(get("/teacher/subjects"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh mục môn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("catalog unavailable"))));
    }

    @Test
    void assignUsesSelectedSubjectAndTrimmedDescriptionsThenRedirects() throws Exception {
        mockMvc.perform(post("/teacher/subjects/assign").with(csrf())
                        .param("subjectId", availableId.toString())
                        .param("levelDescription", "  Lớp 6–12  ")
                        .param("experienceDescription", "  Luyện thi  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/subjects"))
                .andExpect(flash().attribute("subjectNotice", "Đã thêm môn dạy vào hồ sơ."));
        verify(teacherSubjects).assignSubject(teacherId, availableId,
                new AssignSubjectRequest("Lớp 6–12", "Luyện thi"));
    }

    @Test
    void duplicateAssignShowsSafeBusinessError() throws Exception {
        doThrow(new BusinessException(ErrorCode.SUBJECT_ALREADY_ASSIGNED))
                .when(teacherSubjects).assignSubject(teacherId, assignedId, new AssignSubjectRequest(null, null));
        mockMvc.perform(post("/teacher/subjects/assign").with(csrf()).param("subjectId", assignedId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("subjectError", "Môn học này đã có trong hồ sơ."));
    }

    @Test
    void missingSubjectSelectionRendersFieldErrorWithoutCallingService() throws Exception {
        mockMvc.perform(post("/teacher/subjects/assign").with(csrf()).param("subjectId", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-subjects"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chọn một môn học.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-describedby=\"subjectId-error\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("autofocus=\"autofocus\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void removeUsesPostAndRedirectsWithConfirmationMessage() throws Exception {
        mockMvc.perform(post("/teacher/subjects/{subjectId}/remove", assignedId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/subjects"))
                .andExpect(flash().attribute("subjectNotice", "Đã gỡ môn dạy khỏi hồ sơ."));
        verify(teacherSubjects).unassignSubject(teacherId, assignedId);
    }

    @Test
    void removeNotAssignedShowsSafeError() throws Exception {
        doThrow(new BusinessException(ErrorCode.SUBJECT_NOT_ASSIGNED))
                .when(teacherSubjects).unassignSubject(teacherId, assignedId);
        mockMvc.perform(post("/teacher/subjects/{subjectId}/remove", assignedId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("subjectError", "Môn học không còn trong hồ sơ. Danh sách đã được tải lại."));
    }
}
