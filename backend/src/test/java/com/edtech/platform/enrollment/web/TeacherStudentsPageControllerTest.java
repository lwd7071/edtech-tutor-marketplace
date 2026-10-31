package com.edtech.platform.enrollment.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.enrollment.dto.StudentPackageDetail;
import com.edtech.platform.enrollment.service.TeacherLearnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherStudentsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherStudentsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherLearnerService learners;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID secondStudentId = UUID.randomUUID();
    private final UUID studentPackageId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
    }

    @Test
    void listGroupsPackagesByLearnerAndUsesPrivateNoStoreResponse() throws Exception {
        var request = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, null, request)).thenReturn(new PageImpl<>(List.of(
                learner(studentId, "Nguyễn An", "Gói Toán", "Toán", "ACTIVE"),
                learner(studentId, "Nguyễn An", "Gói Lý", "Vật lý", "COMPLETED"),
                learner(secondStudentId, "Trần Bình", "Gói Hóa", "Hóa học", "ACTIVE")), request, 3));
        mockMvc.perform(get("/teacher/students"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-students"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2 gói học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói Toán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trần Bình")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void listShowsActionableEmptyState() throws Exception {
        var request = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, null, request)).thenReturn(new PageImpl<>(List.of(), request, 0));
        mockMvc.perform(get("/teacher/students"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có học viên")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/packages")));
    }

    @Test
    void listFailureShowsRetryWithoutLeakingException() throws Exception {
        var request = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, null, request)).thenThrow(new IllegalStateException("private failure"));
        mockMvc.perform(get("/teacher/students"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tải lại học viên")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private failure"))));
    }

    @Test
    void detailShowsOnlyPackageDataForSelectedStudent() throws Exception {
        var request = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, studentId, request)).thenReturn(new PageImpl<>(List.of(
                learner(studentId, "Nguyễn An", "Gói Toán nâng cao", "Toán", "ACTIVE")), request, 1));
        mockMvc.perform(get("/teacher/students/{studentId}", studentId))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-student-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói Toán nâng cao")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Còn lại")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã tiêu thụ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1.200.000 ₫")));
        verify(learners).list(teacherId, studentId, request);
    }

    @Test
    void studentWithoutPackagesForCurrentTeacherReturnsNotFound() throws Exception {
        var request = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, studentId, request)).thenReturn(new PageImpl<>(List.of(), request, 0));
        mockMvc.perform(get("/teacher/students/{studentId}", studentId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("workspace/teacher-student-detail"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy học viên")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quay lại danh sách học viên")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"success\""))));
    }

    @Test
    void detailReadFailureOffersRetryForSameOwnedPath() throws Exception {
        var request = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, studentId, request)).thenThrow(new IllegalStateException("private failure"));
        mockMvc.perform(get("/teacher/students/{studentId}", studentId))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được thông tin gói")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/students/" + studentId)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private failure"))));
    }

    @Test
    void outOfRangePageRedirectsToLastAvailablePage() throws Exception {
        var request = PageRequest.of(8, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(learners.list(teacherId, null, request)).thenReturn(new PageImpl<>(List.of(), request, 15));
        mockMvc.perform(get("/teacher/students").param("page", "8"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/students?page=1"));
    }

    private TeacherLearnerService.LearnerPackage learner(UUID owner, String name, String packageName, String subject, String status) {
        StudentPackageDetail detail = new StudentPackageDetail(studentPackageId, UUID.randomUUID(), UUID.randomUUID(), packageName,
                10, 7, 1_200_000, null, Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"),
                status, UUID.randomUUID(), owner, UUID.randomUUID(), 1, 2, 0, null, 0);
        return new TeacherLearnerService.LearnerPackage(owner, name, subject, detail);
    }
}
