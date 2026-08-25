package com.edtech.platform.admin.service;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.auth.facade.IdentityModerationFacade;
import com.edtech.platform.subject.facade.SubjectApprovalFacade;
import com.edtech.platform.teacher.facade.TeacherApprovalFacade;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalChange;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminApprovalServiceTest {
    @Mock TeacherApprovalFacade teachers;
    @Mock SubjectApprovalFacade subjects;
    @Mock IdentityModerationFacade identities;
    @Mock AuditLogService auditLogs;

    @Test
    void approvingTeacherWritesAuditInTheSameUseCase() {
        UUID actor = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        TeacherApprovalSnapshot before = new TeacherApprovalSnapshot(teacherId, UUID.randomUUID(),
                "PENDING_APPROVAL", null, null, null);
        TeacherApprovalSnapshot after = new TeacherApprovalSnapshot(teacherId, before.userId(),
                "APPROVED", null, actor, java.time.Instant.now());
        when(teachers.approve(teacherId, actor)).thenReturn(new TeacherApprovalChange(before, after));
        AdminApprovalService service = new AdminApprovalService(teachers, subjects, identities,
                auditLogs, new AuditSnapshotMapper());

        service.approveTeacher(teacherId, actor, "Đủ hồ sơ", new AuditContext("127.0.0.1", "test"));

        verify(auditLogs).append(eq(actor), eq(AuditAction.TEACHER_APPROVED), eq("TEACHER_PROFILE"), eq(teacherId),
                anyMap(), anyMap(), org.mockito.ArgumentMatchers.any(AuditContext.class));
    }
}
