package com.edtech.platform.finance.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.dto.request.CreateExtensionRequest;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentExtensionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private ExtensionService extensionService;
    @InjectMocks private StudentExtensionController controller;

    private final UUID studentId = UUID.randomUUID();

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
                        return new AuthenticatedUser(studentId, "student@example.com", "STUDENT");
                    }
                })
                .build();
    }

    @Test
    void createExtension_shouldReturn201() throws Exception {
        UUID pkgId = UUID.randomUUID();
        Instant requested = Instant.now().plusSeconds(86400 * 30);
        CreateExtensionRequest req = new CreateExtensionRequest(pkgId, "Reason", requested);

        ExtensionRequestView view = new ExtensionRequestView(
                UUID.randomUUID(), pkgId, studentId, "Reason", requested, null,
                ExtensionStatus.PENDING, null, null, null, Instant.now()
        );
        when(extensionService.createExtension(eq(studentId), any())).thenReturn(view);

        mockMvc.perform(post("/api/student/extension-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listExtensions_shouldReturn200Paged() throws Exception {
        Instant requested = Instant.now().plusSeconds(86400 * 30);
        ExtensionRequestView view = new ExtensionRequestView(
                UUID.randomUUID(), UUID.randomUUID(), studentId, "Reason", requested, null,
                ExtensionStatus.PENDING, null, null, null, Instant.now()
        );
        when(extensionService.findStudentExtensions(eq(studentId), any()))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/student/extension-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }
}