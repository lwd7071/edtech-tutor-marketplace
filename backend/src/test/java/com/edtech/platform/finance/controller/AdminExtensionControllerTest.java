package com.edtech.platform.finance.controller;

import com.edtech.platform.admin.dto.request.ApproveExtensionRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.domain.ExtensionStatus;
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminExtensionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private ExtensionService extensionService;
    @InjectMocks private AdminExtensionController controller;

    private final UUID adminId = UUID.randomUUID();

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
                        return new AuthenticatedUser(adminId, "admin@example.com", "ADMIN");
                    }
                })
                .build();
    }

    @Test
    void listAdminExtensions_shouldReturn200Paged() throws Exception {
        Instant requested = Instant.now().plusSeconds(86400 * 30);
        ExtensionRequestView view = new ExtensionRequestView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Reason", requested, null,
                ExtensionStatus.PENDING, null, null, null, Instant.now()
        );
        when(extensionService.findAdminExtensions(nullable(String.class), any()))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/admin/extension-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void approveExtension_shouldReturn200() throws Exception {
        UUID extId = UUID.randomUUID();
        Instant approved = Instant.now().plusSeconds(86400 * 30);
        ApproveExtensionRequest req = new ApproveExtensionRequest(approved, "Duyet");

        ExtensionRequestView view = new ExtensionRequestView(
                extId, UUID.randomUUID(), UUID.randomUUID(), "Reason", approved, approved,
                ExtensionStatus.APPROVED, "Duyet", adminId, Instant.now(), Instant.now()
        );
        when(extensionService.approveExtension(eq(adminId), eq(extId), any())).thenReturn(view);

        mockMvc.perform(post("/api/admin/extension-requests/" + extId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }
}