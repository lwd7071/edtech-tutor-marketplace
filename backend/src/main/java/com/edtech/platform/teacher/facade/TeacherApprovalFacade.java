package com.edtech.platform.teacher.facade;

import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TeacherApprovalFacade {
    Page<TeacherApprovalSnapshot> findPendingApprovals(Pageable pageable);
    TeacherApprovalSnapshot approve(UUID teacherProfileId, UUID adminId);
    TeacherApprovalSnapshot reject(UUID teacherProfileId, String reason);
}
