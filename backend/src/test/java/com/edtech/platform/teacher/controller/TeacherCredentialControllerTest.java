package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.dto.TeacherCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import com.edtech.platform.common.storage.CredentialProofStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherCredentialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TeacherCredentialService service;

    @InjectMocks
    private TeacherCredentialController controller;

    private final UUID teacherUserId = UUID.randomUUID();

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
                        return new AuthenticatedUser(teacherUserId, "teacher@example.com", "TEACHER");
                    }
                })
                .build();
    }

    @Test
    void list_returns200AndCredentialList() throws Exception {
        UUID credId = UUID.randomUUID();
        TeacherCredentialView view = new TeacherCredentialView(
                credId, "IELTS 8.0", CredentialStatus.APPROVED,
                "/api/teacher/credentials/" + credId + "/proof", null, Instant.now(), 0L
        );
        when(service.list(teacherUserId)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/teacher/credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(credId.toString()))
                .andExpect(jsonPath("$.data[0].label").value("IELTS 8.0"))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));

        verify(service).list(teacherUserId);
    }

    @Test
    void create_returns201AndCreatedCredential() throws Exception {
        UUID credId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        TeacherCredentialView view = new TeacherCredentialView(
                credId, "TOEIC 990", CredentialStatus.PENDING,
                "/api/teacher/credentials/" + credId + "/proof", null, null, 0L
        );
        MockMultipartFile file = new MockMultipartFile(
                "proof", "cert.pdf", "application/pdf", "%PDF".getBytes()
        );

        when(service.create(eq(teacherUserId), eq("TOEIC 990"), any()))
                .thenReturn(new TeacherCredentialService.CredentialResult(teacherId, view));

        mockMvc.perform(multipart("/api/teacher/credentials")
                        .file(file)
                        .param("label", "TOEIC 990"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(credId.toString()))
                .andExpect(jsonPath("$.data.label").value("TOEIC 990"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(service).create(eq(teacherUserId), eq("TOEIC 990"), any());
    }

    @Test
    void delete_returns204NoContent() throws Exception {
        UUID credId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(service.delete(teacherUserId, credId, 1L)).thenReturn(teacherId);

        mockMvc.perform(delete("/api/teacher/credentials/" + credId)
                        .param("version", "1"))
                .andExpect(status().isNoContent());

        verify(service).delete(teacherUserId, credId, 1L);
    }

    @Test
    void proof_returns200WithNoStoreHeader() throws Exception {
        UUID credId = UUID.randomUUID();
        byte[] content = "%PDF-proof-content".getBytes();
        when(service.proof(teacherUserId, credId))
                .thenReturn(new CredentialProofStorage.DownloadedFile(content, "application/pdf"));

        mockMvc.perform(get("/api/teacher/credentials/" + credId + "/proof"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Type", "application/pdf"));

        verify(service).proof(teacherUserId, credId);
    }
}
