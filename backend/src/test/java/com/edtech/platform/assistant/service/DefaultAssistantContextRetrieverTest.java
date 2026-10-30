package com.edtech.platform.assistant.service;

import com.edtech.platform.catalog.dto.SubjectDto;
import com.edtech.platform.catalog.dto.TeacherCard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAssistantContextRetrieverTest {
    @Test
    void tutorSearchReturnsOnlyPublicMarketplaceFactsAndProfileLink() {
        var teacherId = java.util.UUID.fromString("5af80338-2a17-4b53-9b64-5d7853e3b0d8");
        var teacher = new TeacherCard(teacherId, "Mai Nguyễn", null, "Dạy Toán THPT", 5, true,
                true, false, List.of(new SubjectDto(java.util.UUID.randomUUID(), "Toán")),
                250000, 4.8, 4.7, 12, 3);
        PublicTeacherSearch search = question -> List.of(teacher);
        var retriever = new DefaultAssistantContextRetriever(search);

        var result = retriever.retrieve("Tìm gia sư Toán giúp mình").orElseThrow();

        assertTrue(result.evidence().contains("Mai Nguyễn"));
        assertTrue(result.evidence().contains("250000"));
        assertEquals("/teachers/" + teacherId, result.sources().get(1).url());
    }

    @Test
    void unsupportedQuestionDoesNotSearchMarketplace() {
        AtomicBoolean searched = new AtomicBoolean();
        var retriever = new DefaultAssistantContextRetriever(question -> {
            searched.set(true);
            return List.of();
        });

        assertTrue(retriever.retrieve("Thủ đô nước Pháp là gì?").isEmpty());
        assertEquals(false, searched.get());
    }
}
