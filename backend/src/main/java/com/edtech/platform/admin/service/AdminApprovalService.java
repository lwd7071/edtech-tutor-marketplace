package com.edtech.platform.admin.service;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.auth.facade.IdentityModerationFacade;
import com.edtech.platform.auth.facade.dto.IdentityModerationChange;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.subject.facade.SubjectApprovalFacade;
import com.edtech.platform.subject.facade.dto.SubjectProposalChange;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import com.edtech.platform.subject.facade.dto.SubjectResolutionCommand;
import com.edtech.platform.teacher.facade.TeacherApprovalFacade;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalChange;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {
    private final TeacherApprovalFacade teachers;
    private final SubjectApprovalFacade subjects;
    private final IdentityModerationFacade identities;
    private final AuditLogService auditLogs;
    private final AuditSnapshotMapper snapshots;

    @Transactional(readOnly = true)
    public Page<TeacherApprovalSnapshot> teacherApprovals(String status, Pageable pageable) {
        return teachers.findApprovals(status, pageable);
    }

    @Transactional
    public TeacherApprovalSnapshot approveTeacher(UUID id, UUID actor, String note, AuditContext context) {
        TeacherApprovalChange change = teachers.approve(id, actor);
        auditLogs.append(actor, AuditAction.TEACHER_APPROVED, "TEACHER_PROFILE", id,
                snapshots.teacher(change.before()), withNote(snapshots.teacher(change.after()), note), context);
        return change.after();
    }

    @Transactional
    public TeacherApprovalSnapshot rejectTeacher(UUID id, UUID actor, String reason, AuditContext context) {
        TeacherApprovalChange change = teachers.reject(id, actor, reason);
        auditLogs.append(actor, AuditAction.TEACHER_REJECTED, "TEACHER_PROFILE", id,
                snapshots.teacher(change.before()), snapshots.teacher(change.after()), context);
        return change.after();
    }

    @Transactional(readOnly = true)
    public Page<SubjectProposalSnapshot> subjectProposals(Pageable pageable) {
        return subjects.findPending(pageable);
    }

    @Transactional
    public SubjectProposalSnapshot approveSubject(UUID id, UUID actor, SubjectResolutionCommand command,
                                                  AuditContext context) {
        SubjectProposalChange change = subjects.approve(id, actor, command);
        auditLogs.append(actor, AuditAction.SUBJECT_PROPOSAL_APPROVED, "SUBJECT_PROPOSAL", id,
                snapshots.subjectProposal(change.before()), snapshots.subjectProposal(change.after()), context);
        return change.after();
    }

    @Transactional
    public SubjectProposalSnapshot rejectSubject(UUID id, UUID actor, String reason, AuditContext context) {
        SubjectProposalChange change = subjects.reject(id, actor, reason);
        auditLogs.append(actor, AuditAction.SUBJECT_PROPOSAL_REJECTED, "SUBJECT_PROPOSAL", id,
                snapshots.subjectProposal(change.before()), snapshots.subjectProposal(change.after()), context);
        return change.after();
    }

    @Transactional
    public IdentitySnapshot changeUserStatus(UUID id, UUID actor, String status, String reason, AuditContext context) {
        IdentityModerationChange change = identities.changeStatus(actor, id, status);
        AuditAction action = "LOCKED".equals(status) ? AuditAction.USER_LOCKED : AuditAction.USER_UNLOCKED;
        auditLogs.append(actor, action, "USER", id, snapshots.user(change.before()),
                withNote(snapshots.user(change.after()), reason), context);
        return change.after();
    }

    private Map<String, Object> withNote(Map<String, Object> source, String note) {
        if (note == null || note.isBlank()) return source;
        Map<String, Object> result = new LinkedHashMap<>(source);
        result.put("moderationNote", note);
        return Map.copyOf(result);
    }
}
