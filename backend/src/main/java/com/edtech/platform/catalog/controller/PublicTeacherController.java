package com.edtech.platform.catalog.controller;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.facade.TeacherFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/teachers")
@Validated
@RequiredArgsConstructor
public class PublicTeacherController {

    private final TeacherMarketplaceService teacherMarketplaceService;
    private final PricingPackageService pricingPackageService;
    private final TeacherFacade teacherFacade;

    @GetMapping
    public ApiResponse<List<TeacherCard>> searchTeachers(@Valid @ModelAttribute TeacherSearchParams params) {
        Page<TeacherCard> page = teacherMarketplaceService.searchTeachers(params);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }

    @GetMapping("/{id}")
    public ApiResponse<TeacherPublicDetail> getTeacherDetail(@PathVariable UUID id) {
        return ApiResponse.ok(teacherMarketplaceService.getTeacherDetail(id));
    }

    @GetMapping("/{id}/packages")
    public ApiResponse<List<PricingPackageView>> getTeacherPackages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải lớn hơn 0") @Max(value = 100, message = "Kích thước tối đa là 100") int size) {
        Page<PricingPackageView> pkgs = pricingPackageService.getTeacherPackages(id, PageRequest.of(page, size));
        return ApiResponse.page(pkgs.getContent(), PageMeta.from(pkgs));
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<List<AvailabilityView>> getTeacherAvailability(@PathVariable UUID id) {
        return ApiResponse.ok(teacherFacade.getPublicAvailability(id));
    }
}
