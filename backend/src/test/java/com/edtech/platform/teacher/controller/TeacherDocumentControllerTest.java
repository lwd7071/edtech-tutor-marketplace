package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.domain.VerificationStatus;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.service.TeacherDocumentService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherDocumentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TeacherDocumentService teacherDocumentService;

    @InjectMocks
    private TeacherDocumentController controller;

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
    void list_shouldReturn200AndDocuments() throws Exception {
        TeacherDocumentView view = new TeacherDocumentView(
                UUID.randomUUID(),
                DocumentType.DEGREE,
                "Bang dai hoc",
                "https://res.cloudinary.com/demo/image/upload/v1/degree.pdf",
                "application/pdf",
                1024L,
                VerificationStatus.PENDING,
                null
        );
        when(teacherDocumentService.getDocuments(teacherUserId)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/teacher/documents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(view.id().toString()))
                .andExpect(jsonPath("$.data[0].title").value("Bang dai hoc"))
                .andExpect(jsonPath("$.data[0].documentType").value("DEGREE"));

        verify(teacherDocumentService).getDocuments(teacherUserId);
    }

    @Test
    void uploadDocument_shouldReturn201AndDocumentView() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "degree.png", "image/png", "dummy-image-content".getBytes()
        );
        TeacherDocumentView view = new TeacherDocumentView(
                UUID.randomUUID(),
                DocumentType.DEGREE,
                "Bang dai hoc",
                "https://res.cloudinary.com/demo/image/upload/v1/degree.png",
                "image/png",
                (long) "dummy-image-content".getBytes().length,
                VerificationStatus.PENDING,
                null
        );

        when(teacherDocumentService.uploadDocument(eq(teacherUserId), any(), eq(DocumentType.DEGREE), eq("Bang dai hoc")))
                .thenReturn(view);

        mockMvc.perform(multipart("/api/teacher/documents")
                        .file(file)
                        .param("documentType", "DEGREE")
                        .param("title", "Bang dai hoc"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(view.id().toString()))
                .andExpect(jsonPath("$.data.secureUrl").value(view.secureUrl()))
                .andExpect(jsonPath("$.data.documentType").value("DEGREE"));

        verify(teacherDocumentService).uploadDocument(eq(teacherUserId), any(), eq(DocumentType.DEGREE), eq("Bang dai hoc"));
    }

    @Test
    void deleteDocument_shouldReturn204() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(delete("/api/teacher/documents/{id}", docId))
                .andExpect(status().isNoContent());

        verify(teacherDocumentService).deleteDocument(teacherUserId, docId);
    }
}
