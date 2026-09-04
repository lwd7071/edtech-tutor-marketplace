package com.edtech.platform.admin.repository;

import com.edtech.platform.admin.domain.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, UUID> {

    @Query("SELECT s FROM PlatformSettings s WHERE s.isSingleton = true")
    Optional<PlatformSettings> findSingleton();
}