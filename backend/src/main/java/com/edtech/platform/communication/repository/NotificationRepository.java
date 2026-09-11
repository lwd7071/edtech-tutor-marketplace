package com.edtech.platform.communication.repository;

import com.edtech.platform.communication.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserId(UUID userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsRead(UUID userId, boolean isRead, Pageable pageable);
    Page<Notification> findByUserIdAndReferenceType(UUID userId, String referenceType, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadAndReferenceType(UUID userId, boolean isRead, String referenceType, Pageable pageable);
    Page<Notification> findByUserIdAndReferenceTypeIn(UUID userId, java.util.Collection<String> referenceTypes, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadAndReferenceTypeIn(UUID userId, boolean isRead, java.util.Collection<String> referenceTypes, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);

    Optional<Notification> findByUserIdAndTypeAndReferenceTypeAndReferenceId(
            UUID userId, String type, String referenceType, UUID referenceId);

    @Modifying
    @Query(value = """
            INSERT INTO notifications(user_id,type,title,content,reference_type,reference_id,is_read)
            VALUES (:userId,:type,:title,:content,:referenceType,:referenceId,false)
            ON CONFLICT (user_id,type,reference_type,reference_id) WHERE reference_id IS NOT NULL DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("type") String type,
                       @Param("title") String title, @Param("content") String content,
                       @Param("referenceType") String referenceType, @Param("referenceId") UUID referenceId);
}
