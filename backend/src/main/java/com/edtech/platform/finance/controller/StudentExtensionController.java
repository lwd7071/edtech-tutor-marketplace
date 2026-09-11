package com.edtech.platform.finance.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.request.CreateExtensionRequest;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import com.edtech.platform.finance.idempotency.FinanceIdempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/extension-requests")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentExtensionController {

    private final ExtensionService extensionService;

    @PostMapping
    @FinanceIdempotent(operation = "STUDENT_EXTENSION_CREATE", responseType = ExtensionRequestView.class)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExtensionRequestView> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateExtensionRequest request
    ) {
        return ApiResponse.created(extensionService.createExtension(user.id(), request));
    }

    @GetMapping
    public ApiResponse<List<ExtensionRequestView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<ExtensionRequestView> result = extensionService.findStudentExtensions(user.id(), PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }
}
