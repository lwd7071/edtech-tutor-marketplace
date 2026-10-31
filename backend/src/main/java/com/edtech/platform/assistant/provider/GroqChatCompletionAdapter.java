package com.edtech.platform.assistant.provider;

import com.edtech.platform.assistant.config.AssistantGroqProperties;
import com.edtech.platform.assistant.dto.AssistantHistoryMessage;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Component
public class GroqChatCompletionAdapter implements GroqChatCompletionPort {
    private static final Logger log = LoggerFactory.getLogger(GroqChatCompletionAdapter.class);
    private final RestClient client;
    private final AssistantGroqProperties properties;

    public GroqChatCompletionAdapter(@Qualifier("groqRestClient") RestClient groqRestClient, AssistantGroqProperties properties) {
        this.client = groqRestClient;
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, List<AssistantHistoryMessage> history, String message) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        history.forEach(item -> messages.add(new ChatMessage(item.role().name().toLowerCase(), item.content())));
        messages.add(new ChatMessage("user", message));
        try {
            GroqResponse response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GroqRequest(properties.getModel(), messages, 0.2, 600))
                    .retrieve()
                    .body(GroqResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
            return response.choices().getFirst().message().content();
        } catch (RestClientException failure) {
            log.warn("Groq chat completion failed ({})", failure.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    private record GroqRequest(String model, List<ChatMessage> messages, double temperature, int max_tokens) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record GroqResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
