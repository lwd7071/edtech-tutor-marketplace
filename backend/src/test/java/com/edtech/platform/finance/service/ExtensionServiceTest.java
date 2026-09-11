package com.edtech.platform.finance.service;

import com.edtech.platform.finance.command.ApproveExtensionCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot;
import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.domain.PackageExtensionRequest;
import com.edtech.platform.finance.dto.request.CreateExtensionRequest;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.repository.PackageExtensionRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtensionServiceTest {

    @Mock private PackageExtensionRequestRepository extensionRequestRepository;
    @Mock private EnrollmentFacade enrollmentFacade;
    @Mock private ApplicationEventPublisher events;

    private ExtensionService extensionService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        extensionService = new ExtensionService(extensionRequestRepository, enrollmentFacade, events);
    }

    private EnrollmentPackageSnapshot mockPackage(String status) {
        return new EnrollmentPackageSnapshot(
                packageId, studentId, UUID.randomUUID(), UUID.randomUUID(), status,
                10, 5, 0, 5, 0, 1000000L,
                new BigDecimal("5.00"), Instant.now().minusSeconds(86400 * 30), Instant.now().minusSeconds(86400), 0L
        );
    }

    @Test
    void createExtension_shouldSucceed_whenLockedExpired() {
        EnrollmentPackageSnapshot pkg = mockPackage("LOCKED_EXPIRED");
        when(enrollmentFacade.inspect(packageId, studentId)).thenReturn(pkg);
        when(extensionRequestRepository.existsByStudentPackageIdAndStatus(packageId, ExtensionStatus.PENDING)).thenReturn(false);

        when(extensionRequestRepository.save(any(PackageExtensionRequest.class))).thenAnswer(inv -> {
            PackageExtensionRequest e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
            return e;
        });

        Instant requested = Instant.now().plusSeconds(86400 * 30);
        CreateExtensionRequest req = new CreateExtensionRequest(packageId, "Benh dot xuat", requested);

        ExtensionRequestView view = extensionService.createExtension(studentId, req);

        assertThat(view.studentPackageId()).isEqualTo(packageId);
        assertThat(view.status()).isEqualTo(ExtensionStatus.PENDING);
        assertThat(view.requestedExpiryDate()).isEqualTo(requested);
    }

    @Test
    void approveExtension_shouldExtendPackage_andSetApproved() {
        UUID extensionId = UUID.randomUUID();
        Instant requested = Instant.now().plusSeconds(86400 * 30);
        PackageExtensionRequest extension = PackageExtensionRequest.create(packageId, studentId, "Reason", requested);
        ReflectionTestUtils.setField(extension, "id", extensionId);
        when(extensionRequestRepository.findByIdForUpdate(extensionId)).thenReturn(Optional.of(extension));

        Instant approved = Instant.now().plusSeconds(86400 * 45);
        ApproveExtensionCommand req = new ApproveExtensionCommand(approved, "Duyet gia han 45 ngay");

        ExtensionRequestView view = extensionService.approveExtension(adminId, extensionId, req);

        assertThat(view.status()).isEqualTo(ExtensionStatus.APPROVED);
        assertThat(view.approvedExpiryDate()).isEqualTo(approved);
        verify(enrollmentFacade).extendPackage(packageId, approved);
    }

    @Test
    void rejectExtension_shouldSetRejected() {
        UUID extensionId = UUID.randomUUID();
        Instant requested = Instant.now().plusSeconds(86400 * 30);
        PackageExtensionRequest extension = PackageExtensionRequest.create(packageId, studentId, "Reason", requested);
        ReflectionTestUtils.setField(extension, "id", extensionId);
        when(extensionRequestRepository.findByIdForUpdate(extensionId)).thenReturn(Optional.of(extension));

        RejectFinanceCommand req = new RejectFinanceCommand("Khong hop le", 0);

        ExtensionRequestView view = extensionService.rejectExtension(adminId, extensionId, req);

        assertThat(view.status()).isEqualTo(ExtensionStatus.REJECTED);
        assertThat(view.adminNote()).isEqualTo("Khong hop le");
    }
}
