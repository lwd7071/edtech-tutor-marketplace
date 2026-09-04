package com.edtech.platform.admin.facade.impl;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.admin.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PlatformSettingsFacadeImpl implements PlatformSettingsFacade {

    private final PlatformSettingsRepository platformSettingsRepository;

    @Override
    public int getBayesianMinimumReviews() {
        return platformSettingsRepository.findSingleton()
                .map(s -> s.getBayesianMinimumReviews())
                .orElse(10);
    }

    @Override
    public BigDecimal getCommissionRate() {
        return platformSettingsRepository.findSingleton()
                .map(s -> s.getCommissionRate())
                .orElse(new BigDecimal("5.00"));
    }
}