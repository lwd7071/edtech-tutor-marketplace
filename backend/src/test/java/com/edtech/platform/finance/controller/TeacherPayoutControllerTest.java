package com.edtech.platform.finance.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.domain.PayoutStatus;
import com.edtech.platform.finance.dto.request.CreatePayoutRequest;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.service.PayoutService;
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
class TeacherPayoutControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private PayoutService payoutService;
    @Mock private com.edtech.platform.common.security.RateLimiterService rateLimiterService;
    @InjectMocks private TeacherPayoutController controller;

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
    void createPayout_shouldReturn201() throws Exception {
        UUID bankAccountId = UUID.randomUUID();
        CreatePayoutRequest req = new CreatePayoutRequest(bankAccountId, 500000L, "Rut tien", 0L);

        PayoutRequestView view = new PayoutRequestView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), bankAccountId,
                500000L, PayoutStatus.PENDING, "Rut tien", null, null, null, null, null, null, 0L, Instant.now()
        );
        when(payoutService.createPayout(eq(teacherUserId), any())).thenReturn(view);

        mockMvc.perform(post("/api/teacher/payout-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amountVnd").value(500000));
    }

    @Test
    void listPayouts_shouldReturn200Paged() throws Exception {
        PayoutRequestView view = new PayoutRequestView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                500000L, PayoutStatus.PENDING, "Rut tien", null, null, null, null, null, null, 0L, Instant.now()
        );
        when(payoutService.findTeacherPayouts(eq(teacherUserId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/teacher/payout-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].amountVnd").value(500000))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }
}