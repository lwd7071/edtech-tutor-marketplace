package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.service.TeacherDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/documents")
@RequiredArgsConstructor
public class TeacherDocumentController {

    private final TeacherDocumentService teacherDocumentService;

    @GetMapping
    public ApiResponse<List<TeacherDocumentView>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(teacherDocumentService.getDocuments(user.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherDocumentView> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "title", required = false) String title,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.created(teacherDocumentService.uploadDocument(user.id(), file, documentType, title));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        teacherDocumentService.deleteDocument(user.id(), id);
    }
}
