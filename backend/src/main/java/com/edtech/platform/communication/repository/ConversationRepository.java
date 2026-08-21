package com.edtech.platform.communication.repository;

import com.edtech.platform.communication.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByTeacherIdAndStudentId(UUID teacherId, UUID studentId);

    @Query("SELECT c FROM Conversation c WHERE (c.teacherId = :userId OR c.studentId = :userId) AND c.isDeleted = false ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}
