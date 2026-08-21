package com.edtech.platform.communication.repository;

import com.edtech.platform.communication.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, UUID clientMessageId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.isDeleted = false ORDER BY m.sentAt DESC")
    Page<Message> findByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP WHERE m.conversationId = :conversationId AND m.senderId != :readerId AND m.readAt IS NULL")
    int markMessagesAsRead(@Param("conversationId") UUID conversationId, @Param("readerId") UUID readerId);
}
