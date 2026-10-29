package com.edtech.platform.communication.web;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.communication.domain.MessageType;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.service.ChatCommandService;
import com.edtech.platform.communication.service.ChatQueryService;
import com.edtech.platform.communication.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentMessagesPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentMessagesPageController.class);
    private static final int CONVERSATION_PAGE_SIZE = 20;
    private static final int MESSAGE_PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);

    private final ChatQueryService chatQueries;
    private final ChatCommandService chatCommands;
    private final ConversationService conversations;
    private final AttachmentService attachments;

    @GetMapping("/student/messages")
    public String page(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) UUID id,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "0") int messagePage,
                       Model model) {
        requireStudent(user);
        int safePage = Math.max(0, Math.min(page, 100_000));
        int safeMessagePage = Math.max(0, Math.min(messagePage, 100_000));
        model.addAttribute("activeWorkspaceNav", "messages");
        model.addAttribute("currentUserId", user.id());
        model.addAttribute("conversationPage", safePage);
        model.addAttribute("messagePage", safeMessagePage);
        model.addAttribute("conversationUnavailable", false);
        model.addAttribute("messageUnavailable", false);
        model.addAttribute("selectedConversationUnavailable", false);
        model.addAttribute("conversationRows", List.of());
        model.addAttribute("messageRows", List.of());
        model.addAttribute("hasPreviousConversations", false);
        model.addAttribute("hasNextConversations", false);
        model.addAttribute("hasOlderMessages", false);
        model.addAttribute("conversationCount", 0L);
        if (!model.containsAttribute("chatClientMessageId")) {
            model.addAttribute("chatClientMessageId", UUID.randomUUID());
        }

        Page<ConversationView> conversationPageResult;
        try {
            conversationPageResult = chatQueries.getConversations(user.id(), PageRequest.of(safePage,
                    CONVERSATION_PAGE_SIZE, Sort.unsorted()));
            if (safePage > 0 && conversationPageResult.getTotalElements() > 0 && conversationPageResult.isEmpty()) {
                return "redirect:/student/messages?page=" + (conversationPageResult.getTotalPages() - 1);
            }
            model.addAttribute("conversationRows", conversationPageResult.getContent().stream().map(this::conversationRow).toList());
            model.addAttribute("conversationCount", conversationPageResult.getTotalElements());
            model.addAttribute("hasPreviousConversations", conversationPageResult.hasPrevious());
            model.addAttribute("hasNextConversations", conversationPageResult.hasNext());
            model.addAttribute("conversationUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Student conversations are unavailable");
            model.addAttribute("conversationUnavailable", true);
            conversationPageResult = Page.empty(PageRequest.of(safePage, CONVERSATION_PAGE_SIZE));
        }

        if (id != null && !Boolean.TRUE.equals(model.getAttribute("conversationUnavailable"))) {
            try {
                ConversationView active = chatQueries.getConversation(id, user.id());
                model.addAttribute("activeConversation", active);
                Page<MessageView> messages = chatQueries.getMessages(id, user.id(),
                        PageRequest.of(safeMessagePage, MESSAGE_PAGE_SIZE, Sort.unsorted()));
                List<MessageView> ordered = new ArrayList<>(messages.getContent());
                Collections.reverse(ordered);
                model.addAttribute("messageRows", ordered.stream().map(message -> messageRow(message, user.id())).toList());
                model.addAttribute("hasOlderMessages", safeMessagePage + 1 < messages.getTotalPages());
                if (safeMessagePage > 0 && messages.getTotalElements() > 0 && messages.isEmpty()) {
                    return "redirect:/student/messages?id=" + id + "&page=" + safePage + "&messagePage=" + (messages.getTotalPages() - 1);
                }
            } catch (BusinessException ex) {
                model.addAttribute("selectedConversationUnavailable", true);
            } catch (RuntimeException ex) {
                log.warn("Student message history is unavailable");
                model.addAttribute("messageUnavailable", true);
            }
        }
        model.addAttribute("selectedConversationId", id);
        return "student/messages";
    }

    @PostMapping("/student/messages/send")
    public String send(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam UUID conversationId,
                       @RequestParam UUID clientMessageId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "0") int messagePage,
                       @RequestParam(required = false, defaultValue = "") String content,
                       @RequestParam(required = false) MultipartFile file,
                       RedirectAttributes redirect) {
        requireStudent(user);
        int safePage = Math.max(0, Math.min(page, 100_000));
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() && (file == null || file.isEmpty())) {
            redirect.addFlashAttribute("chatActionError", "Nhập tin nhắn hoặc chọn tệp trước khi gửi.");
            return redirectToConversation(conversationId, safePage, 0);
        }
        try {
            ChatMessageRequest command = new ChatMessageRequest();
            command.setClientMessageId(clientMessageId);
            command.setConversationId(conversationId);
            command.setContent(text.isEmpty() ? null : text);
            if (file != null && !file.isEmpty()) {
                // Verify membership before creating an unbound upload owned by this student.
                chatQueries.getMessages(conversationId, user.id(), PageRequest.of(0, 1));
                AttachmentView attachment = attachments.uploadAttachment(user.id(), AttachableType.MESSAGE, file);
                command.setAttachmentId(attachment.getId());
                command.setMessageType(attachment.getMimeType() != null && attachment.getMimeType().startsWith("image/")
                        ? MessageType.IMAGE : MessageType.FILE);
            } else {
                command.setMessageType(MessageType.TEXT);
            }
            chatCommands.sendMessage(user.id(), command);
            redirect.addFlashAttribute("chatNotice", "Tin nhắn đã được gửi.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("chatActionError", messageFor(ex.getErrorCode()));
            redirect.addFlashAttribute("chatDraft", text);
            redirect.addFlashAttribute("chatClientMessageId", clientMessageId);
            if (file != null && !file.isEmpty()) redirect.addFlashAttribute("chatAttachmentName", file.getOriginalFilename());
        } catch (RuntimeException ex) {
            log.warn("Student message could not be sent");
            redirect.addFlashAttribute("chatActionError", "Chưa gửi được tin nhắn. Hãy thử lại.");
            redirect.addFlashAttribute("chatDraft", text);
            redirect.addFlashAttribute("chatClientMessageId", clientMessageId);
            if (file != null && !file.isEmpty()) redirect.addFlashAttribute("chatAttachmentName", file.getOriginalFilename());
        }
        return redirectToConversation(conversationId, safePage, 0);
    }

    @PostMapping("/student/messages/{id}/read")
    public String markRead(@AuthenticationPrincipal AuthenticatedUser user,
                           @org.springframework.web.bind.annotation.PathVariable UUID id,
                           @RequestParam(defaultValue = "0") int page,
                           RedirectAttributes redirect) {
        requireStudent(user);
        try {
            chatCommands.readMessages(id, user.id());
            redirect.addFlashAttribute("chatNotice", "Đã đánh dấu tin nhắn là đã đọc.");
        } catch (RuntimeException ex) {
            log.warn("Student messages could not be marked read");
            redirect.addFlashAttribute("chatActionError", "Chưa cập nhật được trạng thái đọc. Hãy thử lại.");
        }
        return redirectToConversation(id, Math.max(0, page), 0);
    }

    @PostMapping("/student/messages/open")
    public String open(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam UUID teacherId,
                       RedirectAttributes redirect) {
        requireStudent(user);
        try {
            UUID conversationId = conversations.openForStudent(teacherId, user.id());
            return "redirect:/student/messages?id=" + conversationId;
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("chatOpenError", ex.getErrorCode() == ErrorCode.CONVERSATION_NOT_ALLOWED
                    ? "Bạn cần có gói học, yêu cầu học thử hoặc buổi học hợp lệ với gia sư này."
                    : "Chưa mở được cuộc trò chuyện. Hãy thử lại.");
            return "redirect:/teachers/" + teacherId;
        } catch (RuntimeException ex) {
            log.warn("Student conversation could not be opened");
            redirect.addFlashAttribute("chatOpenError", "Chưa mở được cuộc trò chuyện. Hãy thử lại.");
            return "redirect:/teachers/" + teacherId;
        }
    }

    private ConversationRow conversationRow(ConversationView conversation) {
        return new ConversationRow(conversation, dateLabel(conversation.getLastMessageAt()));
    }

    private MessageRow messageRow(MessageView message, UUID userId) {
        return new MessageRow(message, dateLabel(message.getSentAt()), userId.equals(message.getSenderId()),
                safeHttpUrl(message.getAttachmentUrl()));
    }

    private String dateLabel(Instant value) {
        return value == null ? "Chưa có thời gian" : DATE_TIME.format(value);
    }

    private String safeHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value);
            if (uri.getHost() != null && uri.getRawUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                return uri.toASCIIString();
            }
        } catch (IllegalArgumentException ignored) {
            // Unsafe attachment references remain plain text.
        }
        return null;
    }

    private String messageFor(ErrorCode errorCode) {
        if (errorCode == ErrorCode.CONVERSATION_NOT_FOUND) return "Không tìm thấy cuộc trò chuyện này.";
        if (errorCode == ErrorCode.MESSAGE_DUPLICATE) return "Tin nhắn đã được gửi trước đó. Hãy tải lại cuộc trò chuyện.";
        if (errorCode == ErrorCode.MESSAGE_TYPE_INVALID) return "Tin nhắn hoặc tệp đính kèm chưa hợp lệ.";
        if (errorCode == ErrorCode.FILE_TOO_LARGE) return "Tệp vượt quá dung lượng cho phép.";
        if (errorCode == ErrorCode.FILE_TYPE_NOT_ALLOWED) return "Định dạng tệp này chưa được hỗ trợ.";
        if (errorCode == ErrorCode.FILE_UPLOAD_FAILED) return "Chưa tải được tệp lên. Hãy thử lại.";
        return "Chưa gửi được tin nhắn. Hãy kiểm tra dữ liệu và thử lại.";
    }

    private String redirectToConversation(UUID id, int page, int messagePage) {
        return "redirect:/student/messages?id=" + id + "&page=" + page + "&messagePage=" + messagePage;
    }

    private void requireStudent(AuthenticatedUser user) {
        if (user == null || !"STUDENT".equals(user.role())) throw new AccessDeniedException("Student workspace only");
    }

    public record ConversationRow(ConversationView data, String lastMessageLabel) {}
    public record MessageRow(MessageView data, String sentLabel, boolean own, String attachmentUrl) {}
}
