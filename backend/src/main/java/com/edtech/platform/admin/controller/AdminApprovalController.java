package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.ApproveSubjectProposalRequest;
import com.edtech.platform.admin.dto.request.ApproveTeacherRequest;
import com.edtech.platform.admin.dto.request.ChangeUserStatusRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.admin.dto.request.RejectSubjectProposalRequest;
import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.admin.dto.response.AdminUserView;
import com.edtech.platform.admin.service.AdminUserDirectoryService;
import com.edtech.platform.common.exception.FieldValidationException;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.subject.facade.dto.SubjectProposalSnapshot;
import com.edtech.platform.teacher.facade.dto.TeacherApprovalSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Validated
@RequiredArgsConstructor
public class AdminApprovalController {
    private static final Set<String> TEACHER_SORT = Set.of("createdAt", "updatedAt", "profileStatus");
    private static final Set<String> SUBJECT_SORT = Set.of("createdAt", "updatedAt", "proposedName");
    private final AdminApprovalService service;
    private final AdminUserDirectoryService userDirectory;

    @GetMapping("/teachers/approvals")
    public ApiResponse<java.util.List<TeacherApprovalSnapshot>> teachers(
            @RequestParam(defaultValue = "PENDING_APPROVAL") String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0") int page, 
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải lớn hơn 0") @Max(value = 100, message = "Kích thước tối đa là 100") int size,
            @RequestParam(defaultValue = "createdAt,asc") String sort) {
        Page<TeacherApprovalSnapshot> result = service.teacherApprovals(status, pageable(page, size, sort, TEACHER_SORT));
        return ApiResponse.page("Lấy danh sách hồ sơ thành công", result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/teachers/{id}/approve")
    public ApiResponse<TeacherApprovalSnapshot> approveTeacher(@PathVariable UUID id,
            @Valid @RequestBody ApproveTeacherRequest body, @AuthenticationPrincipal AuthenticatedUser actor,
            HttpServletRequest request) {
        return ApiResponse.ok("Duyệt hồ sơ giáo viên thành công",
                service.approveTeacher(id, actor.id(), body.note(), context(request)));
    }

    @PostMapping("/teachers/{id}/reject")
    public ApiResponse<TeacherApprovalSnapshot> rejectTeacher(@PathVariable UUID id,
            @Valid @RequestBody RejectRequest body, @AuthenticationPrincipal AuthenticatedUser actor,
            HttpServletRequest request) {
        return ApiResponse.ok("Từ chối hồ sơ giáo viên thành công",
                service.rejectTeacher(id, actor.id(), body.reason(), context(request)));
    }

    @GetMapping("/subject-proposals")
    public ApiResponse<java.util.List<SubjectProposalSnapshot>> subjects(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0") int page, 
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải lớn hơn 0") @Max(value = 100, message = "Kích thước tối đa là 100") int size,
            @RequestParam(defaultValue = "createdAt,asc") String sort) {
        Page<SubjectProposalSnapshot> result = service.subjectProposals(pageable(page, size, sort, SUBJECT_SORT));
        return ApiResponse.page("Lấy danh sách đề xuất thành công", result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/subject-proposals/{id}/approve")
    public ApiResponse<SubjectProposalSnapshot> approveSubject(@PathVariable UUID id,
            @Valid @RequestBody ApproveSubjectProposalRequest body,
            @AuthenticationPrincipal AuthenticatedUser actor, HttpServletRequest request) {
        return ApiResponse.ok("Duyệt đề xuất môn học thành công",
                service.approveSubject(id, actor.id(), body.toCommand(), context(request)));
    }

    @PostMapping("/subject-proposals/{id}/reject")
    public ApiResponse<SubjectProposalSnapshot> rejectSubject(@PathVariable UUID id,
            @Valid @RequestBody RejectSubjectProposalRequest body, @AuthenticationPrincipal AuthenticatedUser actor,
            HttpServletRequest request) {
        return ApiResponse.ok("Từ chối đề xuất môn học thành công",
                service.rejectSubject(id, actor.id(), body.reason(), body.version(), context(request)));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<IdentitySnapshot> changeUserStatus(@PathVariable UUID id,
            @Valid @RequestBody ChangeUserStatusRequest body,
            @AuthenticationPrincipal AuthenticatedUser actor, HttpServletRequest request) {
        return ApiResponse.ok("Cập nhật trạng thái tài khoản thành công",
                service.changeUserStatus(id, actor.id(), body.status(), body.reason(), context(request)));
    }

    @GetMapping("/users")
    public ApiResponse<java.util.List<AdminUserView>> users(
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tối đa 100 UTF-16 units") String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải lớn hơn 0") @Max(value = 100, message = "Kích thước tối đa là 100") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (keyword != null && keyword.length() > 100)
            throw new FieldValidationException("keyword", "Từ khóa tối đa 100 UTF-16 units");
        String parsedRole = role == null ? null : switch (role) {
            case "STUDENT" -> "STUDENT";
            case "TEACHER" -> "TEACHER";
            default -> throw new FieldValidationException("role", "Vai trò chỉ nhận STUDENT hoặc TEACHER");
        };
        String parsedStatus = status == null ? null : switch (status) {
            case "ACTIVE" -> "ACTIVE";
            case "LOCKED" -> "LOCKED";
            default -> throw new FieldValidationException("status", "Trạng thái chỉ nhận ACTIVE hoặc LOCKED");
        };
        Page<AdminUserView> result = userDirectory.findUsers(keyword, parsedRole, parsedStatus,
                pageable(page, size, sort, Set.of("createdAt", "fullName", "email", "role", "status", "lastLoginAt")));
        return ApiResponse.page("Lấy danh sách người dùng thành công", result.getContent(), PageMeta.from(result));
    }

    private Pageable pageable(int page, int size, String rawSort, Set<String> allowed) {
        String[] parts = rawSort.split(",", -1);
        if (parts.length != 2 || !allowed.contains(parts[0]))
            throw new FieldValidationException("sort", "Trường sắp xếp không hợp lệ");
        Sort.Direction direction;
        try { direction = Sort.Direction.fromString(parts[1]); }
        catch (IllegalArgumentException exception) {
            throw new FieldValidationException("sort", "Chiều sắp xếp chỉ nhận asc hoặc desc");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0], "id"));
    }

    private AuditContext context(HttpServletRequest request) {
        return new AuditContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
