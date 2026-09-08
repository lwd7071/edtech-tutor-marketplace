package com.edtech.platform.communication.service;

import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.domain.Message;
import com.edtech.platform.communication.domain.MessageType;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.event.ChatMessageCommitted;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceTest {
    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock AttachmentFacade attachments;
    @Mock TeacherFacade teachers;
    @Mock ApplicationEventPublisher events;

    @Test
    void sendPersistsThenPublishesDeliveryEvent() {
        UUID profileId = UUID.randomUUID(), teacherAccountId = UUID.randomUUID(), studentId = UUID.randomUUID();
        Conversation conversation = Conversation.builder().teacherId(profileId).studentId(studentId).build();
        ReflectionTestUtils.setField(conversation, "id", UUID.randomUUID());
        ChatMessageRequest request = new ChatMessageRequest();
        request.setConversationId(conversation.getId());
        request.setClientMessageId(UUID.randomUUID());
        request.setMessageType(MessageType.TEXT);
        request.setContent("Xin chào");
        when(conversations.findByIdForUpdate(conversation.getId())).thenReturn(Optional.of(conversation));
        when(teachers.getTeacher(profileId)).thenReturn(new TeacherSnapshot(
                profileId, teacherAccountId, "APPROVED", true, true,
                "Teacher", null, null, 1, true, false, List.of(), null, null));
        when(messages.findBySenderIdAndClientMessageId(studentId, request.getClientMessageId()))
                .thenReturn(Optional.empty());
        when(messages.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
            return message;
        });
        ChatCommandService service = new ChatCommandService(
                conversations, messages, attachments, new ChatMembership(teachers),
                new MessageViewMapper(attachments), events);

        service.sendMessage(studentId, request);

        ArgumentCaptor<ChatMessageCommitted> event = ArgumentCaptor.forClass(ChatMessageCommitted.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().senderId()).isEqualTo(studentId);
        assertThat(event.getValue().receiverId()).isEqualTo(teacherAccountId);
        assertThat(event.getValue().message().getContent()).isEqualTo("Xin chào");
    }
}
