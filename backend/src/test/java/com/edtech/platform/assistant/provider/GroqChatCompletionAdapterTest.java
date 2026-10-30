package com.edtech.platform.assistant.provider;

import com.edtech.platform.assistant.dto.AssistantHistoryMessage;
import com.edtech.platform.assistant.config.AssistantGroqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;

class GroqChatCompletionAdapterTest {
    @Test
    void sendsGroundedConversationToGroqChatCompletionsAndReturnsAnswer() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AssistantGroqProperties properties = new AssistantGroqProperties();
        properties.setApiKey("test-key");
        properties.setModel("openai/gpt-oss-20b");
        GroqChatCompletionAdapter adapter = new GroqChatCompletionAdapter(builder.build(), properties);
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"model":"openai/gpt-oss-20b","messages":[
                          {"role":"system","content":"use source"},
                          {"role":"user","content":"Tìm gia sư Toán"},
                          {"role":"assistant","content":"Có gia sư Toán"},
                          {"role":"user","content":"Gợi ý giúp mình"}
                        ]}
                        """))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Mình tìm được gia sư phù hợp."}}]}
                        """, MediaType.APPLICATION_JSON));

        String answer = adapter.complete("use source", List.of(
                new AssistantHistoryMessage(AssistantHistoryMessage.Role.USER, "Tìm gia sư Toán"),
                new AssistantHistoryMessage(AssistantHistoryMessage.Role.ASSISTANT, "Có gia sư Toán")),
                "Gợi ý giúp mình");

        assertEquals("Mình tìm được gia sư phù hợp.", answer);
        server.verify();
    }

    @Test
    void mapsGroqServiceUnavailableToRetryableApplicationError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AssistantGroqProperties properties = new AssistantGroqProperties();
        properties.setApiKey("test-key");
        GroqChatCompletionAdapter adapter = new GroqChatCompletionAdapter(builder.build(), properties);
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> adapter.complete("source", List.of(), "question"));

        assertEquals(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, failure.getErrorCode());
        server.verify();
    }

    @Test
    void missingApiKeyFailsClosedWithoutCallingGroq() {
        AssistantGroqProperties properties = new AssistantGroqProperties();
        RestClient client = RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()).build();
        GroqChatCompletionAdapter adapter = new GroqChatCompletionAdapter(client, properties);

        BusinessException failure = assertThrows(BusinessException.class,
                () -> adapter.complete("source", List.of(), "question"));

        assertEquals(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, failure.getErrorCode());
    }
}
