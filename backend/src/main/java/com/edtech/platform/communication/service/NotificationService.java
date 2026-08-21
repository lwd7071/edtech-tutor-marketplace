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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(UUID userId, String type, String title, String content, String referenceType, UUID referenceId) {
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
        
        NotificationView view = mapToView(notification);
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", view);
    }
    
    @Transactional(readOnly = true)
    public Page<NotificationView> getNotifications(UUID userId, Boolean isRead, Pageable pageable) {
        Page<Notification> page;
        if (isRead != null) {
            page = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        } else {
            page = notificationRepository.findByUserId(userId, pageable);
        }
        return page.map(this::mapToView);
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
        return NotificationView.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
