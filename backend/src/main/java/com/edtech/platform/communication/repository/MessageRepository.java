package com.edtech.platform.communication.repository;

import com.edtech.platform.communication.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    interface ConversationSummary {
        UUID getConversationId();
        String getLastMessagePreview();
        long getUnreadCount();
    }

    Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, UUID clientMessageId);
    Optional<Message> findFirstByConversationIdAndDeletedFalseOrderBySentAtDesc(UUID conversationId);
    long countByConversationIdAndSenderIdNotAndReadAtIsNullAndDeletedFalse(UUID conversationId, UUID readerId);

    @Query(value = """
            SELECT c.id AS conversationId,
                   (SELECT m.content FROM messages m
                    WHERE m.conversation_id = c.id AND m.is_deleted = false
                    ORDER BY m.sent_at DESC, m.id DESC LIMIT 1) AS lastMessagePreview,
                   (SELECT count(*) FROM messages m
                    WHERE m.conversation_id = c.id AND m.sender_id <> :readerId
                      AND m.read_at IS NULL AND m.is_deleted = false) AS unreadCount
            FROM conversations c
            WHERE c.id IN (:conversationIds)
            """, nativeQuery = true)
    List<ConversationSummary> summarizeConversations(
            @Param("conversationIds") Collection<UUID> conversationIds,
            @Param("readerId") UUID readerId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.deleted = false ORDER BY m.sentAt DESC, m.id DESC")
    Page<Message> findByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP WHERE m.conversationId = :conversationId AND m.senderId != :readerId AND m.readAt IS NULL")
    int markMessagesAsRead(@Param("conversationId") UUID conversationId, @Param("readerId") UUID readerId);
}
