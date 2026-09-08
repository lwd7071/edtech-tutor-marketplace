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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(UUID id);
    Optional<Conversation> findByTeacherIdAndStudentId(UUID teacherId, UUID studentId);

    @Query(value = "SELECT c.* FROM conversations c JOIN teacher_profiles t ON t.id = c.teacher_id WHERE (t.user_id = :userId OR c.student_id = :userId) AND c.is_deleted = false ORDER BY c.last_message_at DESC NULLS LAST, c.id DESC",
            countQuery = "SELECT count(*) FROM conversations c JOIN teacher_profiles t ON t.id = c.teacher_id WHERE (t.user_id = :userId OR c.student_id = :userId) AND c.is_deleted = false", nativeQuery = true)
    Page<Conversation> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}
