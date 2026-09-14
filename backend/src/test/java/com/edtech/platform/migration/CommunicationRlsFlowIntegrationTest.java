package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CommunicationRlsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V36 Cross-Table Flow: Conversation -> Text Message -> Attachment -> Attachment Message -> Notification -> Read Receipt")
    void verifyCommunicationFlowWithRlsEnabled() {
        UUID studentUserId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID textMsgId = UUID.randomUUID();
        UUID textClientMsgId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID attachMsgId = UUID.randomUUID();
        UUID attachClientMsgId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        String studentEmail = "comm_std_" + studentUserId.toString().substring(0, 8) + "@test.com";
        String teacherEmail = "comm_tch_" + teacherUserId.toString().substring(0, 8) + "@test.com";

        try {
            // 1. Setup Student, Teacher and TeacherProfile
            jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Student Comm', 'STUDENT', 'ACTIVE')",
                    studentUserId, studentEmail);
            jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Teacher Comm', 'TEACHER', 'ACTIVE')",
                    teacherUserId, teacherEmail);
            jdbcTemplate.update("INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible) VALUES (?, ?, 'Teacher Bio', 5, 'APPROVED', true, true)",
                    teacherProfileId, teacherUserId);

            // 2. Open Conversation between Student and Teacher
            jdbcTemplate.update("INSERT INTO public.conversations (id, teacher_id, student_id, last_message_at) VALUES (?, ?, ?, now())",
                    conversationId, teacherProfileId, studentUserId);

            // 3. Student sends first text message
            jdbcTemplate.update("INSERT INTO public.messages (id, conversation_id, sender_id, client_message_id, message_type, content) " +
                            "VALUES (?, ?, ?, ?, 'TEXT', 'Em chao thay a')",
                    textMsgId, conversationId, studentUserId, textClientMsgId);

            // 4. Student uploads an attachment (e.g. homework draft / syllabus inquiry)
            jdbcTemplate.update("INSERT INTO public.attachments (id, owner_id, attachable_type, attachable_id, secure_url, original_filename, mime_type, file_size) " +
                            "VALUES (?, ?, 'MESSAGE', ?, 'https://res.cloudinary.com/test/raw/upload/bai-tap.pdf', 'bai-tap.pdf', 'application/pdf', 2048576)",
                    attachmentId, studentUserId, attachMsgId);

            // 5. Student sends message with attachment
            jdbcTemplate.update("INSERT INTO public.messages (id, conversation_id, sender_id, client_message_id, message_type, content, attachment_id) " +
                            "VALUES (?, ?, ?, ?, 'FILE', 'Em gui thay file bai tap', ?)",
                    attachMsgId, conversationId, studentUserId, attachClientMsgId, attachmentId);

            // Update conversation last_message_at
            jdbcTemplate.update("UPDATE public.conversations SET last_message_at = now() WHERE id = ?", conversationId);

            // 6. System emits notification to Teacher
            jdbcTemplate.update("INSERT INTO public.notifications (id, user_id, type, title, content, reference_type, reference_id, is_read) " +
                            "VALUES (?, ?, 'CHAT_MESSAGE', 'Tin nhan moi', 'Hoc vien da gui tin nhan va tai lieu', 'CONVERSATION', ?, false)",
                    notificationId, teacherUserId, conversationId);

            // 7. Verify all records exist and queryable via backend connection
            Map<String, Object> conv = jdbcTemplate.queryForMap(
                    "SELECT id, teacher_id, student_id, last_message_at FROM public.conversations WHERE id = ?", conversationId);
            assertThat(conv.get("teacher_id")).isEqualTo(teacherProfileId);
            assertThat(conv.get("student_id")).isEqualTo(studentUserId);
            assertThat(conv.get("last_message_at")).isNotNull();

            Long msgCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM public.messages WHERE conversation_id = ?", Long.class, conversationId);
            assertThat(msgCount).isEqualTo(2L);

            Map<String, Object> attach = jdbcTemplate.queryForMap(
                    "SELECT original_filename, mime_type, file_size FROM public.attachments WHERE id = ?", attachmentId);
            assertThat(attach.get("original_filename")).isEqualTo("bai-tap.pdf");
            assertThat(attach.get("mime_type")).isEqualTo("application/pdf");

            // 8. Teacher reads messages and acknowledges notification
            int markedRead = jdbcTemplate.update(
                    "UPDATE public.messages SET read_at = now() WHERE conversation_id = ? AND sender_id = ?", conversationId, studentUserId);
            assertThat(markedRead).isEqualTo(2);

            int notifRead = jdbcTemplate.update(
                    "UPDATE public.notifications SET is_read = true WHERE id = ?", notificationId);
            assertThat(notifRead).isEqualTo(1);

            // 9. Verify updated state
            Long unreadMessages = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM public.messages WHERE conversation_id = ? AND read_at IS NULL", Long.class, conversationId);
            assertThat(unreadMessages).isEqualTo(0L);

            Boolean notifStatus = jdbcTemplate.queryForObject(
                    "SELECT is_read FROM public.notifications WHERE id = ?", Boolean.class, notificationId);
            assertThat(notifStatus).isTrue();

        } finally {
            // Clean up in reverse dependency order
            jdbcTemplate.update("DELETE FROM public.notifications WHERE id = ?", notificationId);
            jdbcTemplate.update("DELETE FROM public.messages WHERE id IN (?, ?)", textMsgId, attachMsgId);
            jdbcTemplate.update("DELETE FROM public.attachments WHERE id = ?", attachmentId);
            jdbcTemplate.update("DELETE FROM public.conversations WHERE id = ?", conversationId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id IN (?, ?)", studentUserId, teacherUserId);
        }
    }
}