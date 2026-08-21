package com.edtech.platform.catalog.controller;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.service.TeacherAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/teachers")
@RequiredArgsConstructor
public class PublicTeacherController {

    private final TeacherMarketplaceService teacherMarketplaceService;
    private final PricingPackageRepository pricingPackageRepository;
    private final TeacherAvailabilityService teacherAvailabilityService;

    @GetMapping
    public ApiResponse<List<TeacherCard>> searchTeachers(@ModelAttribute TeacherSearchParams params) {
        return ApiResponse.ok(teacherMarketplaceService.searchTeachers(params));
    }

    @GetMapping("/{id}")
    public ApiResponse<TeacherPublicDetail> getTeacherDetail(@PathVariable UUID id) {
        return ApiResponse.ok(teacherMarketplaceService.getTeacherDetail(id));
    }

    @GetMapping("/{id}/packages")
    public ApiResponse<List<PricingPackageView>> getTeacherPackages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<com.edtech.platform.catalog.domain.PricingPackage> pkgs = pricingPackageRepository.findByTeacherIdAndStatus(id, PackageStatus.ACTIVE, PageRequest.of(page, size));
        
        List<PricingPackageView> views = pkgs.stream()
                .map(pkg -> new PricingPackageView(
                        pkg.getId(),
                        pkg.getSubject().getId(),
                        pkg.getSubject().getName(),
                        pkg.getName(),
                        pkg.getDescription(),
                        pkg.getTotalSessions(),
                        pkg.getDurationDays(),
                        pkg.getPriceVnd(),
                        pkg.getSessionDurationMinutes(),
                        pkg.getStatus(),
                        pkg.getVersion()
                )).collect(Collectors.toList());
        return ApiResponse.ok(views);
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<List<AvailabilityView>> getTeacherAvailability(@PathVariable UUID id) {
        return ApiResponse.ok(teacherAvailabilityService.getAvailabilitiesByTeacherId(id));
    }
}
