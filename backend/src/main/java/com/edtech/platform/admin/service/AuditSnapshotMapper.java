package com.edtech.platform.admin.service;

import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuditSnapshotMapper {

    public Map<String, Object> user(IdentitySnapshot identity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("userId", identity.id());
        snapshot.put("role", identity.roleName());
        snapshot.put("status", identity.statusName());
        return Map.copyOf(snapshot);
    }

    public Map<String, Object> teacher(TeacherApprovalSnapshot teacher) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("teacherProfileId", teacher.teacherProfileId());
        snapshot.put("userId", teacher.userId());
        snapshot.put("status", teacher.status());
        put(snapshot, "rejectionReason", teacher.rejectionReason());
        put(snapshot, "approvedBy", teacher.approvedBy());
        put(snapshot, "approvedAt", teacher.approvedAt());
        return Map.copyOf(snapshot);
    }

    public Map<String, Object> subjectProposal(SubjectProposalSnapshot proposal) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("proposalId", proposal.proposalId());
        snapshot.put("teacherId", proposal.teacherId());
        snapshot.put("proposedName", proposal.proposedName());
        snapshot.put("status", proposal.status());
        put(snapshot, "reviewNote", proposal.reviewNote());
        put(snapshot, "reviewedBy", proposal.reviewedBy());
        put(snapshot, "reviewedAt", proposal.reviewedAt());
        put(snapshot, "subjectId", proposal.subjectId());
        return Map.copyOf(snapshot);
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }
}
