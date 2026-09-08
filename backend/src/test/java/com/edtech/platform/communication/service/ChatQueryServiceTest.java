package com.edtech.platform.communication.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {
    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock AttachmentFacade attachments;
    @Mock IdentityFacade identities;
    @Mock TeacherFacade teachers;

    @Test
    void studentSeesTeacherAccountAsParticipantNotProfileId() {
        UUID profileId = UUID.randomUUID(), accountId = UUID.randomUUID(), studentId = UUID.randomUUID();
        Conversation conversation = conversation(profileId, studentId);
        PageRequest pageable = PageRequest.of(0, 20);
        when(conversations.findByUserId(studentId, pageable)).thenReturn(new PageImpl<>(List.of(conversation)));
        when(teachers.getTeachers(any())).thenReturn(Map.of(profileId, teacher(profileId, accountId)));
        when(identities.getIdentities(any())).thenReturn(Map.of());
        when(messages.summarizeConversations(any(), eq(studentId))).thenReturn(List.of());

        ConversationView result = service().getConversations(studentId, pageable).getContent().getFirst();

        assertThat(result.getParticipantId()).isEqualTo(accountId);
    }

    @Test
    void conversationListBatchesParticipantPreviewAndUnreadCount() {
        UUID teacherId = UUID.randomUUID(), studentId = UUID.randomUUID();
        Conversation conversation = conversation(teacherId, studentId);
        PageRequest pageable = PageRequest.of(0, 20);
        MessageRepository.ConversationSummary summary = mock(MessageRepository.ConversationSummary.class);
        when(summary.getConversationId()).thenReturn(conversation.getId());
        when(summary.getLastMessagePreview()).thenReturn("Chào thầy");
        when(summary.getUnreadCount()).thenReturn(2L);
        when(conversations.findByUserId(teacherId, pageable))
                .thenReturn(new PageImpl<>(List.of(conversation), pageable, 1));
        when(teachers.getTeachers(any())).thenReturn(Map.of(teacherId, teacher(teacherId, teacherId)));
        when(identities.getIdentities(any())).thenReturn(Map.of(studentId, new IdentitySnapshot(
                studentId, "student@example.com", "Nguyễn An", "STUDENT", "ACTIVE",
                "https://cdn.example.com/avatar.jpg", false, null)));
        when(messages.summarizeConversations(any(), eq(teacherId))).thenReturn(List.of(summary));

        ConversationView result = service().getConversations(teacherId, pageable).getContent().getFirst();

        assertThat(result.getParticipantId()).isEqualTo(studentId);
        assertThat(result.getParticipantName()).isEqualTo("Nguyễn An");
        assertThat(result.getParticipantAvatar()).isEqualTo("https://cdn.example.com/avatar.jpg");
        assertThat(result.getLastMessagePreview()).isEqualTo("Chào thầy");
        assertThat(result.getUnreadCount()).isEqualTo(2);
    }

    private ChatQueryService service() {
        return new ChatQueryService(
                conversations, messages, identities, new ChatMembership(teachers), new MessageViewMapper(attachments));
    }

    private Conversation conversation(UUID teacherId, UUID studentId) {
        Conversation conversation = Conversation.builder().teacherId(teacherId).studentId(studentId).build();
        ReflectionTestUtils.setField(conversation, "id", UUID.randomUUID());
        return conversation;
    }

    private TeacherSnapshot teacher(UUID profileId, UUID accountId) {
        return new TeacherSnapshot(profileId, accountId, "APPROVED", true, true,
                "Teacher", null, null, 1, true, false, List.of(), null, null);
    }
}
