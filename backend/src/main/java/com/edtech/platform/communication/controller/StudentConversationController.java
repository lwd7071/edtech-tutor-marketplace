package com.edtech.platform.communication.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.communication.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/conversations")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentConversationController {
    private final ConversationService conversations;

    public record ConversationOpenResponse(UUID id) {}

    @PutMapping("/teachers/{teacherId}")
    public ApiResponse<ConversationOpenResponse> open(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID teacherId) {
        return ApiResponse.ok(new ConversationOpenResponse(
                conversations.openForStudent(teacherId, user.id()).getId()));
    }
}
