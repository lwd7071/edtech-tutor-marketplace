package com.edtech.platform.catalog.controller;

import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.AdministrativeWard;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/locations")
@RequiredArgsConstructor
public class PublicAdministrativeLocationController {
    private final AdministrativeLocationService service;

    @GetMapping("/provinces")
    public ApiResponse<List<AdministrativeProvince>> getProvinces() {
        return ApiResponse.ok(service.getProvinces());
    }

    @GetMapping("/provinces/{provinceCode}/wards")
    public ApiResponse<List<AdministrativeWard>> getWards(@PathVariable String provinceCode) {
        return ApiResponse.ok(service.getWards(provinceCode));
    }
}
