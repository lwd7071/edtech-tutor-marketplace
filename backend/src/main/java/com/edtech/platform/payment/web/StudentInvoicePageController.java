package com.edtech.platform.payment.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.payment.dto.InvoiceDetail;
import com.edtech.platform.payment.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentInvoicePageController {
    private static final Logger log = LoggerFactory.getLogger(StudentInvoicePageController.class);
    private final InvoiceService invoices;
    private final RateLimiterService rateLimiter;

    @PostMapping("/student/invoices")
    public String create(@AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam UUID pricingPackageId,
                         @RequestParam UUID idempotencyKey,
                         @RequestParam(required = false) String returnTo,
                         RedirectAttributes redirect) {
        String safeReturnPath = safeTeacherPath(returnTo);
        try {
            rateLimiter.checkRateLimit("create_invoice", user.id().toString(), 10, 60);
            InvoiceDetail invoice = invoices.createInvoiceAndPaymentLink(user.id(), pricingPackageId, idempotencyKey);
            return "redirect:/student/checkout/" + invoice.id();
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("purchaseError", "Chưa tạo được hóa đơn. Gói học có thể đã ngừng mở bán hoặc hệ thống đang bận.");
        } catch (RuntimeException ex) {
            log.warn("Student invoice could not be created");
            redirect.addFlashAttribute("purchaseError", "Chưa tạo được hóa đơn. Vui lòng thử lại sau.");
        }
        return "redirect:" + safeReturnPath + "#packages";
    }

    private String safeTeacherPath(String value) {
        if (value == null || !value.startsWith("/teachers/")) return "/teachers";
        try {
            UUID.fromString(value.substring("/teachers/".length()));
            return value;
        } catch (IllegalArgumentException invalid) {
            return "/teachers";
        }
    }
}
