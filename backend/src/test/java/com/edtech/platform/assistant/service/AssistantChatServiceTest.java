package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantChatRequest;
import com.edtech.platform.assistant.dto.AssistantChatResponse;
import com.edtech.platform.assistant.dto.AssistantOutcome;
import com.edtech.platform.assistant.dto.AssistantSource;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantChatServiceTest {
    @Test
    void unsupportedQuestionReturnsAdminContactWithoutCallingGroq() {
        AtomicBoolean providerCalled = new AtomicBoolean();
        AssistantChatService service = new AssistantChatService(
                (system, history, message) -> {
                    providerCalled.set(true);
                    return "unexpected";
                },
                question -> Optional.empty());

        AssistantChatResponse response = service.reply(new AssistantChatRequest("Thủ đô nước Pháp là gì?", List.of()));

        assertEquals(AssistantOutcome.CONTACT_ADMIN, response.outcome());
        assertEquals("Mình chưa có thông tin phù hợp để giải đáp câu hỏi này. Bạn có thể liên hệ admin qua luongvietvidong@gmail.com để được hỗ trợ nhé.", response.reply());
        assertEquals(List.of(), response.sources());
        org.junit.jupiter.api.Assertions.assertFalse(providerCalled.get());
    }

    @Test
    void supportedQuestionReturnsAnswerAndGroundedSources() {
        AssistantSource source = new AssistantSource("Cách hoạt động", "/how-it-works");
        AssistantChatService service = new AssistantChatService(
                (system, history, message) -> "Sau khi mua gói, bạn gửi yêu cầu đặt buổi và gia sư sẽ xác nhận giờ học.",
                question -> Optional.of(new AssistantContext("Sau khi mua gói, học viên gửi yêu cầu đặt buổi.", List.of(source))));

        AssistantChatResponse response = service.reply(new AssistantChatRequest("Mình muốn đặt lịch học", List.of()));

        assertEquals(AssistantOutcome.ANSWERED, response.outcome());
        assertEquals("Sau khi mua gói, bạn gửi yêu cầu đặt buổi và gia sư sẽ xác nhận giờ học.", response.reply());
        assertEquals(List.of(source), response.sources());
    }

    @Test
    void modelFallbackIsReturnedAsContactAdminOutcome() {
        AssistantChatService service = new AssistantChatService(
                (system, history, message) -> "Mình chưa có thông tin phù hợp để giải đáp câu hỏi này. Bạn có thể liên hệ admin qua luongvietvidong@gmail.com để được hỗ trợ nhé.",
                question -> Optional.of(new AssistantContext("Hướng dẫn tìm gia sư.", List.of(new AssistantSource("Tìm gia sư", "/teachers")))));

        AssistantChatResponse response = service.reply(new AssistantChatRequest("Câu hỏi khác", List.of()));

        assertEquals(AssistantOutcome.CONTACT_ADMIN, response.outcome());
        assertEquals(List.of(), response.sources());
    }

    @Test
    void publicTutorSearchWithNoMatchesReturnsMarketplaceGuidanceWithoutCallingGroq() {
        AtomicBoolean providerCalled = new AtomicBoolean();
        AssistantChatService service = new AssistantChatService(
                (system, history, message) -> {
                    providerCalled.set(true);
                    return "unexpected";
                },
                question -> Optional.of(new AssistantContext("", List.of(
                        new AssistantSource("Tìm gia sư", "/teachers")),
                        "Mình chưa tìm thấy gia sư phù hợp trong danh sách công khai hiện tại.")));

        AssistantChatResponse response = service.reply(new AssistantChatRequest("Tìm gia sư Toán", List.of()));

        assertEquals(AssistantOutcome.ANSWERED, response.outcome());
        assertEquals("Mình chưa tìm thấy gia sư phù hợp trong danh sách công khai hiện tại.", response.reply());
        assertEquals("/teachers", response.sources().getFirst().url());
        org.junit.jupiter.api.Assertions.assertFalse(providerCalled.get());
    }
}
