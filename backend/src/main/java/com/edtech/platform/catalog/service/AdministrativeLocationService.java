package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.AdministrativeProvince;
import com.edtech.platform.catalog.dto.AdministrativeWard;
import com.edtech.platform.catalog.repository.AdministrativeLocationRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdministrativeLocationService {
    private final AdministrativeLocationRepository repository;

    @Transactional(readOnly = true)
    public List<AdministrativeProvince> getProvinces() {
        return repository.findActiveProvinces();
    }

    @Transactional(readOnly = true)
    public List<AdministrativeWard> getWards(String provinceCode) {
        if (provinceCode == null || !provinceCode.matches("^[0-9]{2}$")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return repository.findActiveWards(provinceCode);
    }
}
