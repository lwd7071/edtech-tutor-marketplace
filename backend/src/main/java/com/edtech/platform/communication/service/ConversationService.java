package com.edtech.platform.communication.service;

import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Conversation getOrCreateConversation(UUID teacherId, UUID studentId) {
        Optional<Conversation> existing = conversationRepository.findByTeacherIdAndStudentId(teacherId, studentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation newConv = Conversation.builder()
                .teacherId(teacherId)
                .studentId(studentId)
                .build();

        try {
            return conversationRepository.save(newConv);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conversation already created for teacher {} and student {} due to concurrent request", teacherId, studentId);
            return conversationRepository.findByTeacherIdAndStudentId(teacherId, studentId)
                    .orElseThrow(() -> new RuntimeException("Could not find conversation after DataIntegrityViolationException"));
        }
    }
}
