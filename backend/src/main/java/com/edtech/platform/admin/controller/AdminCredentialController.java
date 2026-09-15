package com.edtech.platform.admin.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.TeacherCredentialView;
import com.edtech.platform.teacher.dto.AdminCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController @RequestMapping("/api/admin/credentials") @RequiredArgsConstructor
public class AdminCredentialController {
    private final TeacherCredentialService service;
    @GetMapping public ApiResponse<java.util.List<AdminCredentialView>> list(@RequestParam(defaultValue = "PENDING") String status) { return ApiResponse.ok(service.adminList(status)); }
    @PostMapping("/{id}/approve") public ApiResponse<TeacherCredentialView> approve(@PathVariable UUID id, @RequestParam long version, @AuthenticationPrincipal AuthenticatedUser admin) { return ApiResponse.ok("Duyệt minh chứng thành công", service.approve(id, admin.id(), version).view()); }
    @PostMapping("/{id}/reject") public ApiResponse<TeacherCredentialView> reject(@PathVariable UUID id, @RequestParam String reason, @RequestParam long version, @AuthenticationPrincipal AuthenticatedUser admin) { return ApiResponse.ok("Từ chối minh chứng thành công", service.reject(id, reason, admin.id(), version).view()); }
    @GetMapping("/{id}/proof") public ResponseEntity<byte[]> proof(@PathVariable UUID id) throws java.io.IOException {
        var file = service.adminProof(id);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(org.springframework.http.MediaType.parseMediaType(file.mimeType())).body(file.bytes());
    }
}
