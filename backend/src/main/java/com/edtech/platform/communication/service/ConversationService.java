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
    private final com.edtech.platform.enrollment.facade.EnrollmentFacade enrollment;
    private final com.edtech.platform.booking.facade.BookingEligibilityFacade bookingEligibility;

    @Transactional
    public Conversation openForStudent(UUID teacherProfileId, UUID studentUserId) {
        Optional<Conversation> existing = conversationRepository.findByTeacherIdAndStudentId(teacherProfileId, studentUserId);
        if (existing.isPresent()) return existing.get();
        if (!enrollment.hasValidRelationship(teacherProfileId, studentUserId)
                && !bookingEligibility.hasValidBookingOrTrial(teacherProfileId, studentUserId)) {
            throw new com.edtech.platform.common.exception.BusinessException(
                    com.edtech.platform.common.exception.ErrorCode.CONVERSATION_NOT_ALLOWED);
        }
        return getOrCreateConversation(teacherProfileId, studentUserId);
    }

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
