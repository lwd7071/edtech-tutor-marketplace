package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantChatRequest;
import com.edtech.platform.assistant.dto.AssistantHistoryMessage;
import com.edtech.platform.assistant.dto.AssistantOutcome;
import com.edtech.platform.assistant.provider.GroqChatCompletionPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AssistantChatServiceTest {
    private AssistantChatService service(String classification, AtomicReference<String> evidence, PublicTeacherSearch search) {
        GroqChatCompletionPort groq = (system, history, message) -> {
            if (system.startsWith("Classify")) return classification;
            evidence.set(system);
            return system.contains("OUT_OF_SCOPE") ? "Contact luongvietvidong@gmail.com" : "Grounded answer";
        };
        return new AssistantChatService(groq, search, new ObjectMapper());
    }

    @Test
    void guidesAreSelectedByRoleAndUnauthorizedTopicNeverReachesGroq() {
        var prompt = new AtomicReference<String>();
        var assistant = service("{\"intent\":\"GUIDE\",\"topics\":[\"admin_refunds\",\"login\"],\"language\":\"en\"}", prompt, q -> List.of());
        var response = assistant.reply(new AssistantChatRequest("How do I log in?", List.of()), "GUEST");
        assertEquals("en", response.language());
        assertEquals(List.of("/auth/login"), response.sources().stream().map(source -> source.url()).toList());
        assertFalse(prompt.get().contains("Quản trị viên"));
        assertFalse(prompt.get().contains("/admin/"));
    }

    @Test
    void eachRoleReceivesOnlyItsOwnGuidance() {
        for (String role : List.of("STUDENT", "TEACHER", "ADMIN")) {
            String id = switch (role) { case "STUDENT" -> "student_booking"; case "TEACHER" -> "teacher_payout"; default -> "admin_approval"; };
            var response = service("{\"intent\":\"GUIDE\",\"topics\":[\"" + id + "\"],\"language\":\"vi\"}", new AtomicReference<>(), q -> List.of())
                    .reply(new AssistantChatRequest("Hướng dẫn giúp tôi", List.of()), role);
            assertEquals(AssistantOutcome.ANSWERED, response.outcome());
            assertTrue(response.sources().getFirst().url().startsWith("/" + role.toLowerCase()));
        }
    }

    @Test
    void tutorFiltersAreValidatedBeforeCatalogSearch() {
        var query = new AtomicReference<String>();
        var assistant = service("{\"intent\":\"TUTOR_SEARCH\",\"topics\":[],\"subject\":\"MATH\",\"level\":\"HIGH_SCHOOL\",\"language\":\"en\"}", new AtomicReference<>(), q -> { query.set(q); return List.of(); });
        var response = assistant.reply(new AssistantChatRequest("Find a high school math tutor", List.of()), "GUEST");
        assertEquals("tìm gia sư Toán THPT", query.get());
        assertEquals("/teachers", response.sources().getFirst().url());
    }

    @Test
    void vagueAndUnrelatedQuestionsHaveDifferentOutcomes() {
        var vague = service("{\"intent\":\"CLARIFY\",\"topics\":[],\"language\":\"vi\"}", new AtomicReference<>(), q -> List.of())
                .reply(new AssistantChatRequest("Giúp mình", List.of()), "STUDENT");
        var unrelated = service("{\"intent\":\"OUT_OF_SCOPE\",\"topics\":[],\"language\":\"en\"}", new AtomicReference<>(), q -> List.of())
                .reply(new AssistantChatRequest("What is the capital of France?", List.of()), "GUEST");
        assertEquals(AssistantOutcome.NEEDS_CLARIFICATION, vague.outcome());
        assertEquals(AssistantOutcome.CONTACT_ADMIN, unrelated.outcome());
        assertTrue(unrelated.reply().contains("luongvietvidong@gmail.com"));
    }

    @Test
    void latestQuestionCanSwitchLanguageWhileHistoryIsPassedToGroq() {
        var seenHistory = new AtomicReference<List<AssistantHistoryMessage>>();
        GroqChatCompletionPort groq = (system, history, message) -> {
            seenHistory.set(history);
            return system.startsWith("Classify")
                    ? "{\"intent\":\"GUIDE\",\"topics\":[\"student_booking\"],\"language\":\"en\"}"
                    : "Send a booking request after buying a package.";
        };
        var history = List.of(new AssistantHistoryMessage(AssistantHistoryMessage.Role.USER, "Đặt lịch thế nào?"));
        var response = new AssistantChatService(groq, q -> List.of(), new ObjectMapper())
                .reply(new AssistantChatRequest("Explain in English", history), "STUDENT");
        assertEquals("en", response.language());
        assertEquals(history, seenHistory.get());
    }

    @Test
    void playbookPathsAreLocalAndRoleScoped() {
        for (var guide : AssistantPlaybook.GUIDES) {
            assertTrue(guide.path().startsWith("/") && !guide.path().startsWith("//"));
            assertFalse(guide.roles().isEmpty());
        }
    }
}
