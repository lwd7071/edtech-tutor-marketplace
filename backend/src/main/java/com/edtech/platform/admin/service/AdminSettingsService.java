package com.edtech.platform.admin.service;

import com.edtech.platform.admin.domain.PlatformSettings;
import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final PlatformSettingsRepository repository;

    @Transactional(readOnly = true)
    public PlatformSettingsView getSettings() {
        PlatformSettings settings = repository.findSingleton()
                .orElseGet(() -> repository.findAll().stream().findFirst()
                        .orElseGet(() -> new PlatformSettings(new BigDecimal("5.00"), 10, 11, 12)));
        return PlatformSettingsView.from(settings);
    }

    @Transactional
    public PlatformSettingsView updateSettings(UpdatePlatformSettingsRequest request) {
        PlatformSettings settings = repository.findSingleton()
                .orElseGet(() -> repository.save(new PlatformSettings(new BigDecimal("5.00"), 10, 11, 12)));
        settings.update(
                request.commissionRate(),
                request.bayesianMinimumReviews(),
                request.bookingReminderHours(),
                request.bookingExpirationHours()
        );
        return PlatformSettingsView.from(repository.save(settings));
    }
}