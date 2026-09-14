package com.edtech.platform.finance.service;

import com.edtech.platform.finance.command.ApproveExtensionCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot;
import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.domain.PackageExtensionRequest;
import com.edtech.platform.finance.dto.request.CreateExtensionRequest;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.repository.PackageExtensionRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import com.edtech.platform.common.event.StudentLifecycleEvent;
import com.edtech.platform.admin.facade.AuditTrailFacade;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ExtensionService {

    private final PackageExtensionRequestRepository extensionRequestRepository;
    private final EnrollmentFacade enrollmentFacade;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final AuditTrailFacade auditTrail;

    @Autowired
    public ExtensionService(PackageExtensionRequestRepository extensionRequestRepository,
                             EnrollmentFacade enrollmentFacade,
                             ApplicationEventPublisher events,
                             Clock clock, AuditTrailFacade auditTrail) {
        this.extensionRequestRepository = extensionRequestRepository;
        this.enrollmentFacade = enrollmentFacade;
        this.events = events;
        this.clock = clock;
        this.auditTrail = auditTrail;
    }

    public ExtensionService(PackageExtensionRequestRepository extensionRequestRepository,
                             EnrollmentFacade enrollmentFacade,
                             ApplicationEventPublisher events) {
        this(extensionRequestRepository, enrollmentFacade, events, Clock.systemUTC(), null);
    }

    @Transactional
    public ExtensionRequestView createExtension(UUID studentId, CreateExtensionRequest request) {
        Instant now = clock.instant();
        if (request == null || request.requestedExpiryDate() == null || !request.requestedExpiryDate().isAfter(now)) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_DATE_INVALID);
        }

        EnrollmentPackageSnapshot pkg = enrollmentFacade.lockForFinanceAction(
                request.studentPackageId(), studentId, request.packageVersion());

        if (!"LOCKED_EXPIRED".equalsIgnoreCase(pkg.status())) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_NOT_ALLOWED);
        }

        if (extensionRequestRepository.existsByStudentPackageIdAndStatus(pkg.id(), ExtensionStatus.PENDING)) {
            throw new BusinessException(ErrorCode.EXTENSION_REQUEST_ALREADY_PENDING);
        }

        PackageExtensionRequest extension = PackageExtensionRequest.create(
                pkg.id(),
                studentId,
                request.reason(),
                request.requestedExpiryDate(),
                now
        );
        extension = extensionRequestRepository.save(extension);

        return ExtensionRequestView.from(extension);
    }

    @Transactional(readOnly = true)
    public Page<ExtensionRequestView> findStudentExtensions(UUID studentId, Pageable pageable) {
        return extensionRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable)
                .map(ExtensionRequestView::from);
    }

    @Transactional(readOnly = true)
    public Page<ExtensionRequestView> findAdminExtensions(String status, Pageable pageable) {
        ExtensionStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try { parsedStatus = ExtensionStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new BusinessException(ErrorCode.VALIDATION_ERROR); }
        }
        return findAdminExtensions(parsedStatus, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ExtensionRequestView> findAdminExtensions(ExtensionStatus status, Pageable pageable) {
        Page<PackageExtensionRequest> page = (status != null)
                ? extensionRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : extensionRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(ExtensionRequestView::from);
    }

    @Transactional
    public ExtensionRequestView approveExtension(UUID adminId, UUID extensionId, ApproveExtensionCommand request) {
        PackageExtensionRequest extension = extensionRequestRepository.findByIdForUpdate(extensionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTENSION_REQUEST_NOT_FOUND));

        if (extension.getStatus() != ExtensionStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTENSION_INVALID_STATE);
        }

        Instant now = clock.instant();
        if (request.approvedExpiryDate() == null || !request.approvedExpiryDate().isAfter(now)) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_DATE_INVALID);
        }

        // 1. Extend package in enrollment module
        enrollmentFacade.extendPackage(extension.getStudentPackageId(), request.approvedExpiryDate());

        // 2. Approve extension
        extension.approve(adminId, request.approvedExpiryDate(), request.adminNote(), now, now);
        if (auditTrail != null) auditTrail.append(adminId, "EXTENSION_APPROVED", "EXTENSION_REQUEST", extensionId,
                java.util.Map.of("status", "PENDING"), java.util.Map.of("status", "APPROVED", "approvedExpiryDate", request.approvedExpiryDate()));
        notifyStudent(extension, "EXTENSION_APPROVED", "Yêu cầu gia hạn đã được duyệt");

        return ExtensionRequestView.from(extension);
    }

    @Transactional
    public ExtensionRequestView rejectExtension(UUID adminId, UUID extensionId, RejectFinanceCommand request) {
        PackageExtensionRequest extension = extensionRequestRepository.findByIdForUpdate(extensionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTENSION_REQUEST_NOT_FOUND));

        if (extension.getStatus() != ExtensionStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTENSION_INVALID_STATE);
        }

        extension.reject(adminId, request.reason(), clock.instant());
        if (auditTrail != null) auditTrail.append(adminId, "EXTENSION_REJECTED", "EXTENSION_REQUEST", extensionId,
                java.util.Map.of("status", "PENDING"), java.util.Map.of("status", "REJECTED"));
        notifyStudent(extension, "EXTENSION_REJECTED", "Yêu cầu gia hạn bị từ chối");
        return ExtensionRequestView.from(extension);
    }

    private void notifyStudent(PackageExtensionRequest extension, String type, String title) {
        events.publishEvent(new StudentLifecycleEvent(extension.getStudentId(), type, title, title,
                "EXTENSION", extension.getId()));
    }
}
