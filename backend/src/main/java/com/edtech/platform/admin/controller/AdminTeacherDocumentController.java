package com.edtech.platform.admin.controller;

import com.edtech.platform.teacher.service.TeacherDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/teacher-documents")
@RequiredArgsConstructor
public class AdminTeacherDocumentController {
    private final TeacherDocumentService service;

    @GetMapping("/{id}/proof")
    public ResponseEntity<byte[]> proof(@PathVariable UUID id) throws IOException {
        var file = service.adminProof(id);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(file.mimeType())).body(file.bytes());
    }
}
