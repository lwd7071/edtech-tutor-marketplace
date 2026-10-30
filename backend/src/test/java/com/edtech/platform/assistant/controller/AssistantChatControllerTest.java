package com.edtech.platform.assistant.controller;

import com.edtech.platform.assistant.dto.AssistantChatRequest;
import com.edtech.platform.assistant.dto.AssistantHistoryMessage;
import com.edtech.platform.assistant.dto.AssistantSource;
import com.edtech.platform.assistant.provider.GroqChatCompletionPort;
import com.edtech.platform.assistant.service.AssistantChatService;
import com.edtech.platform.assistant.service.AssistantContext;
import com.edtech.platform.assistant.service.AssistantContextRetriever;
import com.edtech.platform.common.security.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantChatControllerTest {
    @Test
    void guestCanAskAndReceivesApiEnvelopeWithPublicSource() throws Exception {
        AssistantContextRetriever context = question -> Optional.of(new AssistantContext("Thông tin công khai", List.of(new AssistantSource("Cách hoạt động", "/how-it-works"))));
        GroqChatCompletionPort groq = (system, history, message) -> "Mở trang cách hoạt động để xem các bước.";
        var service = new AssistantChatService(groq, context);
        AtomicReference<String> identifier = new AtomicReference<>();
        RateLimiterService limiter = new RateLimiterService(null) {
            @Override
            public void checkRateLimit(String action, String key, int maxRequests, long windowSeconds) {
                identifier.set(key);
                org.junit.jupiter.api.Assertions.assertEquals("assistant_chat", action);
                org.junit.jupiter.api.Assertions.assertEquals(10, maxRequests);
                org.junit.jupiter.api.Assertions.assertEquals(300, windowSeconds);
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AssistantChatController(service, limiter)).build();

        mvc.perform(post("/api/public/assistant/chat")
                        .with(request -> { request.setRemoteAddr("203.0.113.7"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Đặt lịch thế nào?","history":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.outcome").value("ANSWERED"))
                .andExpect(jsonPath("$.data.sources[0].url").value("/how-it-works"))
                .andExpect(header().string("Cache-Control", "no-store"));

        org.junit.jupiter.api.Assertions.assertEquals("203.0.113.7", identifier.get());
    }

    @Test
    void emptyQuestionIsRejectedByThePublicApi() throws Exception {
        AssistantChatService service = new AssistantChatService(
                (system, history, message) -> "unexpected",
                question -> Optional.empty());
        RateLimiterService limiter = new RateLimiterService(null) {
            @Override
            public void checkRateLimit(String action, String key, int maxRequests, long windowSeconds) {
                throw new AssertionError("invalid requests must be rejected before rate limiting");
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AssistantChatController(service, limiter))
                .setControllerAdvice(new com.edtech.platform.common.exception.GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/public/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\",\"history\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }
}
