package com.edtech.platform.catalog.facade.impl;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
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
    @Mock
    private TeacherFacade teacherFacade;
    @InjectMocks
    private PricingPackageFacadeImpl facade;

    @Test
    void returnsSnapshotOnlyForActivePackage() {
        UUID id = UUID.randomUUID();
        PricingPackage pricingPackage = packageWithStatus(PackageStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(pricingPackage));
        when(teacherFacade.getTeacher(pricingPackage.getTeacherId()))
                .thenReturn(teacher("APPROVED", true));

        var snapshot = facade.getPurchasablePackage(id);

        assertThat(snapshot.teacherId()).isEqualTo(pricingPackage.getTeacherId());
        assertThat(snapshot.priceVnd()).isEqualTo(500_000L);
        assertThat(snapshot.status()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsInactivePackage() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(packageWithStatus(PackageStatus.INACTIVE)));

        assertThatThrownBy(() -> facade.getPurchasablePackage(id))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_ACTIVE));
    }

    @Test
    void rejectsNotFoundOrSoftDeletedPackage() {
        UUID id = UUID.randomUUID();
        // @Where makes a soft-deleted package indistinguishable from not-found at this facade boundary.
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> facade.getPurchasablePackage(id))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRICING_PACKAGE_NOT_FOUND));
    }

    @Test
    void rejectsActivePackage_whenTeacherIsNotApproved() {
        for (String status : java.util.List.of("DRAFT", "PENDING_APPROVAL", "REJECTED")) {
            UUID id = UUID.randomUUID();
            PricingPackage pricingPackage = packageWithStatus(PackageStatus.ACTIVE);
            when(repository.findById(id)).thenReturn(Optional.of(pricingPackage));
            when(teacherFacade.getTeacher(pricingPackage.getTeacherId()))
                    .thenReturn(teacher(status, true));

            assertThatThrownBy(() -> facade.getPurchasablePackage(id))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_ACTIVE));
        }
    }

    @Test
    void rejectsActivePackage_whenApprovedTeacherIsHiddenOrMissing() {
        UUID hiddenId = UUID.randomUUID();
        PricingPackage hiddenPackage = packageWithStatus(PackageStatus.ACTIVE);
        when(repository.findById(hiddenId)).thenReturn(Optional.of(hiddenPackage));
        when(teacherFacade.getTeacher(hiddenPackage.getTeacherId()))
                .thenReturn(teacher("APPROVED", false));

        assertThatThrownBy(() -> facade.getPurchasablePackage(hiddenId))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_ACTIVE));

        UUID missingId = UUID.randomUUID();
        PricingPackage missingTeacherPackage = packageWithStatus(PackageStatus.ACTIVE);
        when(repository.findById(missingId)).thenReturn(Optional.of(missingTeacherPackage));
        when(teacherFacade.getTeacher(missingTeacherPackage.getTeacherId())).thenReturn(null);

        assertThatThrownBy(() -> facade.getPurchasablePackage(missingId))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_ACTIVE));
    }

    @Test
    void returnsInactivePackageForExistingInvoiceFulfillment_withoutRecheckingSaleEligibility() {
        UUID id = UUID.randomUUID();
        PricingPackage pricingPackage = packageWithStatus(PackageStatus.INACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(pricingPackage));

        var snapshot = facade.getPackageForPaymentFulfillment(id);

        assertThat(snapshot.status()).isEqualTo("INACTIVE");
        assertThat(snapshot.teacherId()).isEqualTo(pricingPackage.getTeacherId());
        org.mockito.Mockito.verifyNoInteractions(teacherFacade);
    }

    private PricingPackage packageWithStatus(PackageStatus status) {
        return PricingPackage.builder()
                .teacherId(UUID.randomUUID()).subjectId(UUID.randomUUID()).name("Gói học")
                .totalSessions(10).durationDays(30).priceVnd(500_000L)
                .sessionDurationMinutes(60).status(status).build();
    }

    private TeacherSnapshot teacher(String status, boolean visible) {
        return new TeacherSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), status, false, visible,
                "Teacher", null, null, 0, true, false, java.util.List.of(), null, null);
    }
}
