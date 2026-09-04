package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.domain.TrialRequest;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.dto.request.RejectTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrialRequestControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TrialRequestService service;

    @InjectMocks
    private TrialRequestController controller;

    private final UUID currentUserId = UUID.randomUUID();

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
                        return new AuthenticatedUser(currentUserId, "user@example.com", "STUDENT");
                    }
                })
                .build();
    }

    @Test
    void createTrialRequest_shouldReturn201() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        CreateTrialRequest req = new CreateTrialRequest(teacherId, subjectId, Instant.now().plusSeconds(7200), "Hello");

        TrialRequest trialRequest = TrialRequest.create(currentUserId, teacherId, subjectId, Instant.now().plusSeconds(7200), "Hello");
        when(service.create(eq(currentUserId), any())).thenReturn(trialRequest);

        mockMvc.perform(post("/api/student/trials/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void acceptTrialRequest_shouldReturn201() throws Exception {
        UUID requestId = UUID.randomUUID();
        AcceptTrialRequest req = new AcceptTrialRequest(Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), DeliveryMode.ONLINE, "meet", null);

        BookingDetail detail = new BookingDetail(
                UUID.randomUUID(), UUID.randomUUID(), currentUserId, UUID.randomUUID(), null,
                req.startTime(), req.endTime(), DeliveryMode.ONLINE, com.edtech.platform.booking.domain.BookingStatus.SCHEDULED,
                true, false, null, 0L
        );
        when(service.accept(eq(currentUserId), eq(requestId), any())).thenReturn(detail);

        mockMvc.perform(post("/api/teacher/trial-requests/" + requestId + "/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.trial").value(true));
    }

    @Test
    void rejectTrialRequest_shouldReturn200() throws Exception {
        UUID requestId = UUID.randomUUID();
        RejectTrialRequest req = new RejectTrialRequest("Busy");

        TrialRequest trialRequest = TrialRequest.create(UUID.randomUUID(), currentUserId, UUID.randomUUID(), Instant.now().plusSeconds(7200), "Hello");
        trialRequest.reject("Busy", Instant.now());
        when(service.reject(eq(currentUserId), eq(requestId), eq("Busy"))).thenReturn(trialRequest);

        mockMvc.perform(post("/api/teacher/trial-requests/" + requestId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}