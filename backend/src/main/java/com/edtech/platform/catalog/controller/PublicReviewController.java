package com.edtech.platform.catalog.controller;

import com.edtech.platform.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/teachers")
public class PublicReviewController {

    @GetMapping("/{id}/reviews")
    public ApiResponse<Page<Object>> getTeacherReviews(@PathVariable UUID id) {
        // TODO: Implement in M7. Returning empty for now.
        return ApiResponse.ok(Page.empty());
    }
}
