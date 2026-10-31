package com.edtech.platform.assistant.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantWidgetLayoutTest {
    @Test
    void allRoleShellsLoadTheAssistantWidgetWithCsrf() throws Exception {
        for (String fragment : List.of("public-header.html", "student-header.html", "teacher-messages-header.html", "admin-workspace-header.html")) {
            String html = Files.readString(Path.of("src/main/resources/templates/fragments", fragment));
            assertTrue(html.contains("/js/assistant-widget.js"), fragment + " must load the assistant widget");
            assertTrue(html.contains("data-csrf"), fragment + " must pass the HTML CSRF token");
        }
    }

    @Test
    void widgetHasAccessiblePrivacyAndFallbackStatesAndResponsiveStyles() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/assistant-widget.js"));
        String css = Files.readString(Path.of("src/main/resources/static/css/assistant-widget.css"));

        assertTrue(script.contains("/assistant/chat"));
        assertTrue(script.contains("X-CSRF-TOKEN"));
        assertTrue(script.contains("aria-live"));
        assertTrue(script.contains("mailto:luongvietvidong@gmail.com"));
        assertTrue(script.contains("Groq"));
        assertTrue(script.contains("student_package" ) || script.contains("Mua gói học thế nào?"));
        assertTrue(script.contains("Duyệt hồ sơ gia sư ở đâu?"));
        assertTrue(css.contains("@media (max-width: 640px)"));
        assertTrue(css.contains("prefers-reduced-motion"));
    }
}
