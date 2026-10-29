package com.edtech.platform.communication.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.communication.domain.MessageType;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.service.ChatCommandService;
import com.edtech.platform.communication.service.ChatQueryService;
import com.edtech.platform.communication.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentMessagesPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentMessagesPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ChatQueryService chatQueries;
    @MockBean private ChatCommandService chatCommands;
    @MockBean private ConversationService conversations;
    @MockBean private AttachmentService attachments;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID clientMessageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()));
    }

    @Test
    void rendersEmptyInboxAsNoScriptServerPage() throws Exception {
        when(chatQueries.getConversations(eq(studentId), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mockMvc.perform(get("/student/messages"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/messages"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có cuộc trò chuyện")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void rendersSelectedConversationAndMessages() throws Exception {
        when(chatQueries.getConversations(eq(studentId), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 21));
        when(chatQueries.getConversation(eq(conversationId), eq(studentId))).thenReturn(conversation());
        when(chatQueries.getMessages(eq(conversationId), eq(studentId), any()))
                .thenReturn(new PageImpl<>(List.of(message("Chào bạn")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/student/messages").param("id", conversationId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gia sư Thu Hà")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chào bạn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("multipart/form-data")));
    }

    @Test
    void textMessageUsesPrincipalAndIdempotencyIdThenRedirects() throws Exception {
        mockMvc.perform(post("/student/messages/send")
                        .param("conversationId", conversationId.toString())
                        .param("clientMessageId", clientMessageId.toString())
                        .param("content", "  Xin chào gia sư  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/messages?id=" + conversationId + "&page=0&messagePage=0"));

        var request = org.mockito.ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(chatCommands).sendMessage(eq(studentId), request.capture());
        org.junit.jupiter.api.Assertions.assertEquals(clientMessageId, request.getValue().getClientMessageId());
        org.junit.jupiter.api.Assertions.assertEquals(MessageType.TEXT, request.getValue().getMessageType());
        org.junit.jupiter.api.Assertions.assertEquals("Xin chào gia sư", request.getValue().getContent());
    }

    @Test
    void attachmentUploadUsesMessageTypeAndDirectCommandService() throws Exception {
        UUID attachmentId = UUID.randomUUID();
        when(attachments.uploadAttachment(eq(studentId), eq(com.edtech.platform.common.domain.AttachableType.MESSAGE), any()))
                .thenReturn(new AttachmentView(attachmentId, "https://files.example.test/picture.png", "picture.png", "image/png", 12L));
        MockMultipartFile file = new MockMultipartFile("file", "picture.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/student/messages/send")
                        .file(file)
                        .param("conversationId", conversationId.toString())
                        .param("clientMessageId", clientMessageId.toString())
                        .param("content", "Xem ảnh này"))
                .andExpect(status().is3xxRedirection());

        verify(chatQueries).getMessages(eq(conversationId), eq(studentId), any());
        verify(attachments).uploadAttachment(eq(studentId), eq(com.edtech.platform.common.domain.AttachableType.MESSAGE), any());
        var request = org.mockito.ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(chatCommands).sendMessage(eq(studentId), request.capture());
        org.junit.jupiter.api.Assertions.assertEquals(attachmentId, request.getValue().getAttachmentId());
        org.junit.jupiter.api.Assertions.assertEquals(MessageType.IMAGE, request.getValue().getMessageType());
    }

    @Test
    void opensExistingTeacherConversationThroughService() throws Exception {
        when(conversations.openForStudent(eq(conversationId), eq(studentId))).thenReturn(conversationId);
        mockMvc.perform(post("/student/messages/open").param("teacherId", conversationId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/messages?id=" + conversationId));
        verify(conversations).openForStudent(conversationId, studentId);
    }

    private ConversationView conversation() {
        return ConversationView.builder().id(conversationId).participantId(teacherUserId)
                .participantName("Gia sư Thu Hà").lastMessagePreview("Tin gần nhất").unreadCount(1)
                .lastMessageAt(Instant.parse("2026-10-28T12:00:00Z")).build();
    }

    private MessageView message(String content) {
        return MessageView.builder().id(UUID.randomUUID()).conversationId(conversationId).senderId(teacherUserId)
                .clientMessageId(UUID.randomUUID()).messageType(MessageType.TEXT).content(content)
                .sentAt(Instant.parse("2026-10-28T12:00:00Z")).build();
    }
}
