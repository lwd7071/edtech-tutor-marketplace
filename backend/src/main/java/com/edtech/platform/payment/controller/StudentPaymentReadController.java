package com.edtech.platform.payment.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.payment.facade.InvoiceReadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.edtech.platform.payment.dto.StudentInvoiceDetail;
import com.edtech.platform.enrollment.dto.StudentPackageDetail;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentPaymentReadController {
    private final InvoiceReadFacade invoices;
    private final EnrollmentFacade enrollment;

    @GetMapping("/invoices/{id}")
    public ResponseEntity<?> invoice(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return invoices.findOwned(id, user.id()).map(i -> (ResponseEntity<?>) ResponseEntity.ok(StudentInvoiceDetail.from(i)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/packages")
    public ResponseEntity<?> packages(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(enrollment.findStudentPackages(user.id(), status, PageRequest.of(page, Math.min(size, 100))).map(StudentPackageDetail::from));
    }

    @GetMapping("/packages/{id}")
    public ResponseEntity<?> packageDetail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return enrollment.findStudentPackage(id, user.id()).map(p -> (ResponseEntity<?>) ResponseEntity.ok(StudentPackageDetail.from(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
