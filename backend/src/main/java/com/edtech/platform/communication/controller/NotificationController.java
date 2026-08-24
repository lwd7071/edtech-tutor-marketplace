package com.edtech.platform.communication.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.communication.dto.notification.NotificationReadAllResponse;
import com.edtech.platform.communication.dto.notification.NotificationView;
import com.edtech.platform.communication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/notifications", "/api/student/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<java.util.List<NotificationView>> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Boolean isRead,
            Pageable pageable) {
        Page<NotificationView> page = notificationService.getNotifications(user.getId(), isRead, pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationView> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return ApiResponse.ok(notificationService.markAsRead(id, user.getId()));
    }

    @PostMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser user) {
        int count = notificationService.markAllAsRead(user.getId());
        return ApiResponse.ok(new NotificationReadAllResponse(count));
    }
}
