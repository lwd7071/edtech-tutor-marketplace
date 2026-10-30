package com.edtech.platform.assistant.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAssistantContextRetrieverTest {
    @Test
    void platformHowToQuestionHasGroundedGuideContext() {
        var retriever = new DefaultAssistantContextRetriever(question -> List.of(), () -> List.of(
                new AssistantKnowledgeArticle("Hướng dẫn sử dụng Tutor Match",
                        "Tutor Match kết nối học viên với gia sư.",
                        List.of("hướng dẫn sử dụng", "dùng website tutor match"), "/how-it-works", 10)));

        var result = retriever.retrieve("Hướng dẫn sử dụng của web là gì?").orElseThrow();

        assertTrue(result.evidence().contains("Tutor Match"));
        assertTrue(result.sources().stream().anyMatch(source -> source.url().equals("/how-it-works")));
    }

    @Test
    void tutorSearchReturnsOnlyPublicMarketplaceFactsAndProfileLink() {
        var teacherId = java.util.UUID.fromString("5af80338-2a17-4b53-9b64-5d7853e3b0d8");
        PublicTeacherSearch search = question -> List.of(new PublicTeacherMatch("Mai Nguyễn", List.of("Toán"),
                List.of("HIGH_SCHOOL"), 5, 250000, 4.8, "/teachers/" + teacherId));
        var retriever = new DefaultAssistantContextRetriever(search, () -> List.of(
                new AssistantKnowledgeArticle("Cách tìm gia sư", "Mở trang Tìm gia sư để lọc theo môn.",
                        List.of("tìm gia sư", "gia sư"), "/teachers", 20)));

        var result = retriever.retrieve("Tìm gia sư Toán giúp mình").orElseThrow();

        assertTrue(result.evidence().contains("Mai Nguyễn"));
        assertTrue(result.evidence().contains("250000"));
        assertTrue(result.evidence().contains("4.8"));
        assertEquals("/teachers/" + teacherId, result.sources().get(1).url());
    }

    @Test
    void unsupportedQuestionDoesNotSearchMarketplace() {
        AtomicBoolean searched = new AtomicBoolean();
        var retriever = new DefaultAssistantContextRetriever(question -> {
            searched.set(true);
            return List.of();
        }, List::of);

        assertTrue(retriever.retrieve("Thủ đô nước Pháp là gì?").isEmpty());
        assertEquals(false, searched.get());
    }

    @Test
    void tutorQuestionWithNoPublicMatchesReturnsMarketplaceLinkInsteadOfAdminFallback() {
        var retriever = new DefaultAssistantContextRetriever(question -> List.of(), List::of);

        var result = retriever.retrieve("Có gia sư nào dạy Toán cấp 3 không?").orElseThrow();

        assertTrue(result.directReply().contains("chưa tìm thấy gia sư"));
        assertTrue(result.sources().stream().anyMatch(source -> source.url().equals("/teachers")));
    }
}
