package com.edtech.platform.communication.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.communication.domain.Notification;
import com.edtech.platform.communication.dto.notification.NotificationView;
import com.edtech.platform.communication.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.edtech.platform.auth.facade.IdentityFacade identityFacade;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(UUID userId, String type, String title, String content, String referenceType, UUID referenceId) {
        if (referenceId != null) {
            int inserted = notificationRepository.insertIfAbsent(userId, type, title, content, referenceType, referenceId);
            if (inserted == 0) return;
            Notification notification = notificationRepository
                    .findByUserIdAndTypeAndReferenceTypeAndReferenceId(userId, type, referenceType, referenceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
            publishAfterCommit(notification);
            return;
        }
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        
        publishAfterCommit(notification);
    }

    private void publishAfterCommit(Notification notification) {
        NotificationView view = mapToView(notification);
        com.edtech.platform.common.transaction.AfterCommit.run(() -> messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(), "/queue/notifications", view));
    }
    
    @Transactional(readOnly = true)
    public Page<NotificationView> getNotifications(UUID userId, Boolean isRead, String referenceType, Pageable pageable) {
        java.util.List<String> referenceTypes = switch (referenceType == null ? "" : referenceType) {
            case "BOOKING" -> java.util.List.of("BOOKING", "TRIAL_REQUEST");
            case "ASSIGNMENT" -> java.util.List.of("ASSIGNMENT", "SUBMISSION");
            case "FINANCE" -> java.util.List.of("INVOICE", "PAYMENT", "REFUND", "EXTENSION");
            case "SYSTEM" -> java.util.List.of("SYSTEM", "PROFILE");
            case "" -> java.util.List.of();
            default -> java.util.List.of(referenceType);
        };
        Page<Notification> page;
        if (isRead != null && !referenceTypes.isEmpty()) {
            page = notificationRepository.findByUserIdAndIsReadAndReferenceTypeIn(userId, isRead, referenceTypes, pageable);
        } else if (isRead != null) {
            page = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        } else if (!referenceTypes.isEmpty()) {
            page = notificationRepository.findByUserIdAndReferenceTypeIn(userId, referenceTypes, pageable);
        } else {
            page = notificationRepository.findByUserId(userId, pageable);
        }
        String role = identityFacade.getIdentity(userId).map(i -> i.roleName()).orElse(null);
        return page.map(n -> mapToView(n, role));
    }
    
    @Transactional
    public NotificationView markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
                
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND); // Che giấu
        }
        
        notification.markAsRead();
        return mapToView(notificationRepository.save(notification));
    }
    
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId);
    }
    
    private NotificationView mapToView(Notification n) {
        return mapToView(n, identityFacade.getIdentity(n.getUserId()).map(i -> i.roleName()).orElse(null));
    }

    public Page<NotificationView> getNotifications(UUID userId, Boolean isRead, Pageable pageable) {
        return getNotifications(userId, isRead, null, pageable);
    }

    private NotificationView mapToView(Notification n, String role) {
        return NotificationView.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .referenceUrl(NotificationRoutes.resolve(role, n.getReferenceType(), n.getReferenceId()))
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
