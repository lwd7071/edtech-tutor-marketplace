package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.service.TeacherDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/documents")
@RequiredArgsConstructor
public class TeacherDocumentController {

    private final TeacherDocumentService teacherDocumentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherDocumentView> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "title", required = false) String title,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.created(teacherDocumentService.uploadDocument(userId, file, documentType, title));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        teacherDocumentService.deleteDocument(userId, id);
    }
}
