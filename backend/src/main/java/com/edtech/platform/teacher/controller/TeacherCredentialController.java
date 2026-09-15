package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.TeacherCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/teacher/credentials") @RequiredArgsConstructor
public class TeacherCredentialController {
    private final TeacherCredentialService service;
    @GetMapping public ApiResponse<List<TeacherCredentialView>> list(@AuthenticationPrincipal AuthenticatedUser user) { return ApiResponse.ok(service.list(user.id())); }
    @PostMapping(consumes = "multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherCredentialView> create(@RequestParam String label, @RequestParam(value = "proof", required = false) MultipartFile proof, @AuthenticationPrincipal AuthenticatedUser user) { return ApiResponse.created(service.create(user.id(), label, proof).view()); }
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponse<TeacherCredentialView> update(@PathVariable UUID id, @RequestParam String label, @RequestParam long version, @RequestParam(value = "proof", required = false) MultipartFile proof, @AuthenticationPrincipal AuthenticatedUser user) { return ApiResponse.ok(service.update(user.id(), id, label, proof, version).view()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam long version, @AuthenticationPrincipal AuthenticatedUser user) { service.delete(user.id(), id, version); }
    @GetMapping("/{id}/proof")
    public ResponseEntity<byte[]> proof(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) throws java.io.IOException {
        var file = service.proof(user.id(), id);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(file.mimeType())).body(file.bytes());
    }
}
