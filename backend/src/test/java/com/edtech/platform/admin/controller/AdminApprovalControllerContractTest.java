package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.ApproveTeacherRequest;
import com.edtech.platform.admin.service.AdminApprovalService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApprovalControllerContractTest {
    private final AdminApprovalService service = mock(AdminApprovalService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminApprovalController(service))
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
}
