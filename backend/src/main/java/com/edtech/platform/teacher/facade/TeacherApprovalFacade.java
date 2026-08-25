package com.edtech.platform.teacher.facade;

import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TeacherApprovalFacade {
    Page<TeacherApprovalSnapshot> findPendingApprovals(Pageable pageable);
    Page<TeacherApprovalSnapshot> findApprovals(String status, Pageable pageable);
    TeacherApprovalChange approve(UUID teacherProfileId, UUID adminId);
    TeacherApprovalChange reject(UUID teacherProfileId, UUID adminId, String reason);
}
