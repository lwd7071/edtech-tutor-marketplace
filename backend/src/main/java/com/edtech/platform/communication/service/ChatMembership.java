package com.edtech.platform.communication.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMembership {
    private final TeacherFacade teachers;

    public UUID teacherAccountId(Conversation conversation) {
        return teachers.getTeacher(conversation.getTeacherId()).userId();
    }

    public Map<UUID, UUID> teacherAccountIds(Collection<Conversation> conversations) {
        Map<UUID, TeacherSnapshot> snapshots = teachers.getTeachers(
                conversations.stream().map(Conversation::getTeacherId).collect(Collectors.toSet()));
        return conversations.stream().collect(Collectors.toMap(
                Conversation::getId,
                conversation -> snapshot(snapshots, conversation.getTeacherId()).userId()));
    }

    public void requireMember(Conversation conversation, UUID accountId) {
        if (!accountId.equals(conversation.getStudentId()) && !accountId.equals(teacherAccountId(conversation))) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
    }

    public UUID otherParticipant(Conversation conversation, UUID accountId) {
        UUID teacherAccountId = teacherAccountId(conversation);
        return teacherAccountId.equals(accountId) ? conversation.getStudentId() : teacherAccountId;
    }

    private TeacherSnapshot snapshot(Map<UUID, TeacherSnapshot> snapshots, UUID teacherId) {
        TeacherSnapshot snapshot = snapshots.get(teacherId);
        if (snapshot == null) throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        return snapshot;
    }
}
