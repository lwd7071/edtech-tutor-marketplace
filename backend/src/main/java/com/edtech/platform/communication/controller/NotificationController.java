package com.edtech.platform.communication.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
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
    public Page<NotificationView> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Boolean isRead,
            Pageable pageable) {
        return notificationService.getNotifications(user.getId(), isRead, pageable);
    }

    @PatchMapping("/{id}/read")
    public NotificationView markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return notificationService.markAsRead(id, user.getId());
    }

    @PostMapping("/read-all")
    public NotificationReadAllResponse markAllAsRead(@AuthenticationPrincipal AuthenticatedUser user) {
        int count = notificationService.markAllAsRead(user.getId());
        return new NotificationReadAllResponse(count);
    }
}
