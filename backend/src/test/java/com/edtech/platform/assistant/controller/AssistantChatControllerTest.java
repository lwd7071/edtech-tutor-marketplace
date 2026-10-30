package com.edtech.platform.assistant.controller;

import com.edtech.platform.assistant.dto.AssistantChatRequest;
import com.edtech.platform.assistant.dto.AssistantChatResponse;
import com.edtech.platform.assistant.dto.AssistantOutcome;
import com.edtech.platform.assistant.service.AssistantChatService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssistantChatControllerTest {
    @Test
    void publicApiIsAlwaysGuestEvenWithBearerPrincipal() throws Exception {
        var assistant = mock(AssistantChatService.class);
        var limiter = mock(RateLimiterService.class);
        when(assistant.reply(any(AssistantChatRequest.class), eq("GUEST")))
                .thenReturn(new AssistantChatResponse(AssistantOutcome.ANSWERED, "Hello", List.of(), "en"));
        var mvc = MockMvcBuilders.standaloneSetup(new AssistantChatController(assistant, limiter)).build();
        mvc.perform(post("/api/public/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\",\"history\":[],\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(assistant).reply(any(AssistantChatRequest.class), eq("GUEST"));
    }

    @Test
    void htmlEndpointUsesPrincipalRoleAndRateLimitIdentity() throws Exception {
        var assistant = mock(AssistantChatService.class);
        var limiter = mock(RateLimiterService.class);
        when(assistant.reply(any(AssistantChatRequest.class), eq("TEACHER")))
                .thenReturn(new AssistantChatResponse(AssistantOutcome.ANSWERED, "Hello", List.of(), "en"));
        UUID id = UUID.randomUUID();
        new HtmlAssistantChatController(assistant, limiter).chat(
                new AssistantChatRequest("Hello", List.of()),
                new AuthenticatedUser(id, "teacher@example.test", "TEACHER"),
                new org.springframework.mock.web.MockHttpServletRequest());
        verify(limiter).checkRateLimit("assistant_chat", id.toString(), 10, 300);
        verify(assistant).reply(any(AssistantChatRequest.class), eq("TEACHER"));
    }
}
