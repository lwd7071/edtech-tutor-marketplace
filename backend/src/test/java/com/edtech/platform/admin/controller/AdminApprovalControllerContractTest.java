package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.ApproveTeacherRequest;
import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AdminUserDirectoryService;
import com.edtech.platform.admin.dto.response.AdminUserView;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.exception.GlobalExceptionHandler;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApprovalControllerContractTest {
    private final AdminApprovalService service = mock(AdminApprovalService.class);
    private final AdminUserDirectoryService userDirectory = mock(AdminUserDirectoryService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminApprovalController(service, userDirectory))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    private final ObjectMapper json = new ObjectMapper();

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void repeatedApprovalReturnsConflictInSharedEnvelope() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, java.util.List.of()));
        when(service.approveTeacher(any(), any(), anyString(), any()))
                .thenThrow(new BusinessException(ErrorCode.TEACHER_APPROVAL_ALREADY_PROCESSED));

        mvc.perform(post("/api/admin/teachers/{id}/approve", UUID.randomUUID())
                        .contentType("application/json")
                        .content(json.writeValueAsBytes(new ApproveTeacherRequest("double click"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.errors[0].code").value("TEACHER_APPROVAL_ALREADY_PROCESSED"))
                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void unsupportedSortFieldReturnsValidationError() throws Exception {
        mvc.perform(get("/api/admin/teachers/approvals").param("sort", "email,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void userDirectoryReturnsSafeTypedPageAndStableSort() throws Exception {
        UUID id = UUID.randomUUID();
        var view = new AdminUserView(id, "Học viên", "student@example.test", "STUDENT", "ACTIVE",
                java.time.Instant.parse("2026-09-01T00:00:00Z"), null);
        when(userDirectory.findUsers(" student ", "STUDENT", "ACTIVE",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))))
                .thenReturn(new PageImpl<>(java.util.List.of(view), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/admin/users").param("keyword", " student ").param("role", "STUDENT")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].parentEmail").doesNotExist())
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void invalidUserSortReportsFieldSpecificValidationEnvelope() throws Exception {
        mvc.perform(get("/api/admin/users").param("sort", "phone,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    @Test
    void userDirectoryRejectsAdminRoleFilterAndUnsupportedStatus() throws Exception {
        mvc.perform(get("/api/admin/users").param("role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
        mvc.perform(get("/api/admin/users").param("status", "PENDING_VERIFICATION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void userDirectoryBoundsKeywordByUtf16Length() throws Exception {
        mvc.perform(get("/api/admin/users").param("keyword", "ă".repeat(99) + "😀"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("keyword"));
    }

    @Test
    void teacherRejectionAcceptsTheTeacherRejectContractWithoutVersion() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, java.util.List.of()));

        mvc.perform(post("/api/admin/teachers/{id}/reject", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"reason\":\"Thiếu chứng chỉ sư phạm\"}"))
                .andExpect(status().isOk());

        verify(service).rejectTeacher(any(), any(), anyString(), any());
    }
}
