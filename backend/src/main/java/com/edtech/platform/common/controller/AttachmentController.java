package com.edtech.platform.common.controller;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public AttachmentView uploadAttachment(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @RequestParam("attachableType") AttachableType attachableType,
            @RequestParam("file") MultipartFile file) {
        
        return attachmentService.uploadAttachment(userDetails.id(), attachableType, file);
    }
}
