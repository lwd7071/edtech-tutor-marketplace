package com.edtech.platform.admin.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.dto.AdminCredentialView;
import com.edtech.platform.teacher.dto.TeacherCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCredentialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TeacherCredentialService service;

    @InjectMocks
    private AdminCredentialController controller;

    private final UUID adminUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new AuthenticatedUser(adminUserId, "admin@example.com", "ADMIN");
                    }
                })
                .build();
    }

    @Test
    void list_returns200AndPendingCredentials() throws Exception {
        UUID credId = UUID.randomUUID(), teacherId = UUID.randomUUID();
        AdminCredentialView view = new AdminCredentialView(
                credId, teacherId, "IELTS 7.5", "/api/admin/credentials/" + credId + "/proof",
                CredentialStatus.PENDING, null, Instant.now(), 0L
        );
        when(service.adminList("PENDING")).thenReturn(List.of(view));

        mockMvc.perform(get("/api/admin/credentials").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(credId.toString()))
                .andExpect(jsonPath("$.data[0].label").value("IELTS 7.5"));

        verify(service).adminList("PENDING");
    }

    @Test
    void approve_returns200AndSuccessMessage() throws Exception {
        UUID credId = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherCredentialView view = new TeacherCredentialView(
                credId, "IELTS 7.5", CredentialStatus.APPROVED,
                "/api/teacher/credentials/" + credId + "/proof", null, Instant.now(), 1L
        );
        when(service.approve(eq(credId), eq(adminUserId), eq(0L)))
                .thenReturn(new TeacherCredentialService.CredentialResult(teacherId, view));

        mockMvc.perform(post("/api/admin/credentials/" + credId + "/approve")
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Duyệt minh chứng thành công"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(service).approve(eq(credId), eq(adminUserId), eq(0L));
    }

    @Test
    void reject_returns200AndSuccessMessage() throws Exception {
        UUID credId = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherCredentialView view = new TeacherCredentialView(
                credId, "IELTS 7.5", CredentialStatus.REJECTED,
                "/api/teacher/credentials/" + credId + "/proof", "Giấy tờ mờ", null, 1L
        );
        when(service.reject(eq(credId), eq("Giấy tờ mờ"), eq(adminUserId), eq(0L)))
                .thenReturn(new TeacherCredentialService.CredentialResult(teacherId, view));

        mockMvc.perform(post("/api/admin/credentials/" + credId + "/reject")
                        .param("reason", "Giấy tờ mờ")
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Từ chối minh chứng thành công"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(service).reject(eq(credId), eq("Giấy tờ mờ"), eq(adminUserId), eq(0L));
    }

    @Test
    void proof_returns200WithNoStoreHeader() throws Exception {
        UUID credId = UUID.randomUUID();
        byte[] content = "%PDF-admin-proof".getBytes();
        when(service.adminProof(credId))
                .thenReturn(new CredentialProofStorage.DownloadedFile(content, "application/pdf"));

        mockMvc.perform(get("/api/admin/credentials/" + credId + "/proof"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Type", "application/pdf"));

        verify(service).adminProof(credId);
    }
}
