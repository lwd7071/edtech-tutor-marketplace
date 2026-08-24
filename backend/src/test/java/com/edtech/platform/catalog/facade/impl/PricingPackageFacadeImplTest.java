package com.edtech.platform.catalog.facade.impl;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingPackageFacadeImplTest {
    @Mock
    private PricingPackageRepository repository;
    @InjectMocks
    private PricingPackageFacadeImpl facade;

    @Test
    void returnsSnapshotOnlyForActivePackage() {
        UUID id = UUID.randomUUID();
        PricingPackage pricingPackage = packageWithStatus(PackageStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(pricingPackage));

        var snapshot = facade.getPurchasablePackage(id);

        assertThat(snapshot.teacherId()).isEqualTo(pricingPackage.getTeacherId());
        assertThat(snapshot.priceVnd()).isEqualTo(500_000L);
    }

    @Test
    void rejectsInactivePackage() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(packageWithStatus(PackageStatus.INACTIVE)));

        assertThatThrownBy(() -> facade.getPurchasablePackage(id))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_ACTIVE));
    }

    private PricingPackage packageWithStatus(PackageStatus status) {
        return PricingPackage.builder()
                .teacherId(UUID.randomUUID()).subjectId(UUID.randomUUID()).name("Gói học")
                .totalSessions(10).durationDays(30).priceVnd(500_000L)
                .sessionDurationMinutes(60).status(status).build();
    }
}
