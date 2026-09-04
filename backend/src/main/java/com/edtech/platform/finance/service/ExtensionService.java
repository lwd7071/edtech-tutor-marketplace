package com.edtech.platform.finance.service;

import com.edtech.platform.admin.dto.request.ApproveExtensionRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot;
import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.domain.PackageExtensionRequest;
import com.edtech.platform.finance.dto.request.CreateExtensionRequest;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.repository.PackageExtensionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtensionService {

    private final PackageExtensionRequestRepository extensionRequestRepository;
    private final EnrollmentFacade enrollmentFacade;

    @Transactional
    public ExtensionRequestView createExtension(UUID studentId, CreateExtensionRequest request) {
        if (request == null || request.requestedExpiryDate() == null || !request.requestedExpiryDate().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_DATE_INVALID);
        }

        EnrollmentPackageSnapshot pkg = enrollmentFacade.inspect(request.studentPackageId(), studentId);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND);
        }

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
                request.requestedExpiryDate()
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
        ExtensionStatus parsedStatus = (status != null && !status.isBlank()) ? ExtensionStatus.valueOf(status.trim().toUpperCase()) : null;
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
    public ExtensionRequestView approveExtension(UUID adminId, UUID extensionId, ApproveExtensionRequest request) {
        PackageExtensionRequest extension = extensionRequestRepository.findByIdForUpdate(extensionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTENSION_REQUEST_NOT_FOUND));

        if (extension.getStatus() != ExtensionStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTENSION_INVALID_STATE);
        }

        if (request.approvedExpiryDate() == null || !request.approvedExpiryDate().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_DATE_INVALID);
        }

        // 1. Extend package in enrollment module
        enrollmentFacade.extendPackage(extension.getStudentPackageId(), request.approvedExpiryDate());

        // 2. Approve extension
        extension.approve(adminId, request.approvedExpiryDate(), request.adminNote(), Instant.now());

        return ExtensionRequestView.from(extension);
    }

    @Transactional
    public ExtensionRequestView rejectExtension(UUID adminId, UUID extensionId, RejectRequest request) {
        PackageExtensionRequest extension = extensionRequestRepository.findByIdForUpdate(extensionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTENSION_REQUEST_NOT_FOUND));

        if (extension.getStatus() != ExtensionStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTENSION_INVALID_STATE);
        }

        extension.reject(adminId, request.reason(), Instant.now());
        return ExtensionRequestView.from(extension);
    }
}