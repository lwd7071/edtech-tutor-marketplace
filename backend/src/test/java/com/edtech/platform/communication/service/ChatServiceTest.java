package com.edtech.platform.communication.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.domain.Message;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock AttachmentFacade attachments;
    @Mock SimpMessagingTemplate messaging;
    @Mock NotificationService notifications;
    @Mock IdentityFacade identities;
    @Mock com.edtech.platform.teacher.facade.TeacherFacade teachers;

    @Test
    void studentSeesTeacherAccountAsParticipantNotProfileId() {
        UUID profileId = UUID.randomUUID(), accountId = UUID.randomUUID(), studentId = UUID.randomUUID();
        Conversation conversation = Conversation.builder().teacherId(profileId).studentId(studentId).build();
        UUID conversationId = UUID.randomUUID();
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        PageRequest pageable = PageRequest.of(0, 20);
        when(conversations.findByUserId(studentId, pageable)).thenReturn(new PageImpl<>(List.of(conversation)));
        ChatService service = new ChatService(conversations, messages, attachments, messaging, notifications, identities, teachers);
        when(teachers.getTeacher(profileId)).thenReturn(new com.edtech.platform.teacher.facade.dto.TeacherSnapshot(profileId, accountId, "APPROVED", true, true, "Teacher", null, null, 1, true, false, List.of(), null, null));
        assertThat(service.getConversations(studentId, pageable).getContent().getFirst().getParticipantId()).isEqualTo(accountId);
    }

    @Test
    void conversationListContainsTheOtherParticipantPreviewAndUnreadCount() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.builder().teacherId(teacherId).studentId(studentId).build();
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        Message latest = Message.builder().conversationId(conversationId).senderId(studentId)
                .clientMessageId(UUID.randomUUID()).content("Chào thầy").build();
        PageRequest pageable = PageRequest.of(0, 20);

        when(conversations.findByUserId(teacherId, pageable)).thenReturn(new PageImpl<>(List.of(conversation), pageable, 1));
        when(identities.getIdentity(studentId)).thenReturn(Optional.of(new IdentitySnapshot(
                studentId, "student@example.com", "Nguyễn An", "STUDENT", "ACTIVE",
                "https://cdn.example.com/avatar.jpg", false, null)));
        when(messages.findFirstByConversationIdAndDeletedFalseOrderBySentAtDesc(conversationId)).thenReturn(Optional.of(latest));
        when(messages.countByConversationIdAndSenderIdNotAndReadAtIsNullAndDeletedFalse(conversationId, teacherId)).thenReturn(2L);

        ChatService service = new ChatService(conversations, messages, attachments, messaging, notifications, identities, teachers);
        when(teachers.getTeacher(teacherId)).thenReturn(new com.edtech.platform.teacher.facade.dto.TeacherSnapshot(teacherId, teacherId, "APPROVED", true, true, "Teacher", null, null, 1, true, false, List.of(), null, null));
        ConversationView result = service.getConversations(teacherId, pageable).getContent().getFirst();

        assertThat(result.getParticipantId()).isEqualTo(studentId);
        assertThat(result.getParticipantName()).isEqualTo("Nguyễn An");
        assertThat(result.getParticipantAvatar()).isEqualTo("https://cdn.example.com/avatar.jpg");
        assertThat(result.getLastMessagePreview()).isEqualTo("Chào thầy");
        assertThat(result.getUnreadCount()).isEqualTo(2);
    }
}

