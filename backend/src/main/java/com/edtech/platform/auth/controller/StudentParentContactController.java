package com.edtech.platform.auth.controller;

import com.edtech.platform.auth.dto.request.UpdateParentContactRequest;
import com.edtech.platform.auth.dto.response.ParentContactResponse;
import com.edtech.platform.auth.service.ParentContactService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/parent-contact")
@RequiredArgsConstructor
public class StudentParentContactController {

    private final ParentContactService parentContacts;

    @GetMapping
    @RequireRole("STUDENT")
    public ApiResponse<ParentContactResponse> getParentContact(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(parentContacts.get(user.id()));
    }

    @PutMapping
    @RequireRole("STUDENT")
    public ApiResponse<ParentContactResponse> updateParentContact(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateParentContactRequest request) {
        
        return ApiResponse.ok(parentContacts.update(user.id(), request));
    }
}
