package com.edtech.platform.admin.service;

import com.edtech.platform.admin.domain.PlatformSettings;
import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.repository.PlatformSettingsRepository;
import com.edtech.platform.admin.facade.AuditTrailFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AdminSettingsService {

    private final PlatformSettingsRepository repository;
    private final AuditTrailFacade auditTrail;

    public AdminSettingsService(PlatformSettingsRepository repository) {
        this(repository, null);
    }

    @Autowired
    public AdminSettingsService(PlatformSettingsRepository repository, AuditTrailFacade auditTrail) {
        this.repository = repository;
        this.auditTrail = auditTrail;
    }

    @Transactional(readOnly = true)
    public PlatformSettingsView getSettings() {
        PlatformSettings settings = repository.findSingleton()
                .orElseGet(() -> repository.findAll().stream().findFirst()
                        .orElseGet(() -> new PlatformSettings(new BigDecimal("5.00"), 10, 11, 12)));
        return PlatformSettingsView.from(settings);
    }

    @Transactional
    public PlatformSettingsView updateSettings(UpdatePlatformSettingsRequest request) {
        return updateSettings(null, request);
    }

    @Transactional
    public PlatformSettingsView updateSettings(java.util.UUID actorId, UpdatePlatformSettingsRequest request) {
        PlatformSettings settings = repository.findSingleton()
                .orElseGet(() -> repository.save(new PlatformSettings(new BigDecimal("5.00"), 10, 11, 12)));
        java.util.Map<String, Object> before = new java.util.LinkedHashMap<>();
        before.put("commissionRate", settings.getCommissionRate());
        before.put("bayesianMinimumReviews", settings.getBayesianMinimumReviews());
        before.put("bookingReminderHours", settings.getBookingReminderHours());
        before.put("bookingExpirationHours", settings.getBookingExpirationHours());
        settings.update(
                request.commissionRate(),
                request.bayesianMinimumReviews(),
                request.bookingReminderHours(),
                request.bookingExpirationHours()
        );
        var saved = repository.save(settings);
        if (auditTrail != null) {
            java.util.Map<String, Object> after = new java.util.LinkedHashMap<>();
            after.put("commissionRate", saved.getCommissionRate());
            after.put("bayesianMinimumReviews", saved.getBayesianMinimumReviews());
            after.put("bookingReminderHours", saved.getBookingReminderHours());
            after.put("bookingExpirationHours", saved.getBookingExpirationHours());
            auditTrail.append(actorId, "PLATFORM_SETTINGS_UPDATED", "PLATFORM_SETTINGS", saved.getId(), before, after);
        }
        return PlatformSettingsView.from(saved);
    }
}
