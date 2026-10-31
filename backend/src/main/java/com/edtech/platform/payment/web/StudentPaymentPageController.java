package com.edtech.platform.payment.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.payment.facade.InvoiceReadFacade;
import com.edtech.platform.payment.facade.dto.InvoiceSnapshot;
import com.edtech.platform.payment.service.support.QrCodeSvgService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentPaymentPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentPaymentPageController.class);
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", VIETNAMESE)
            .withZone(VIETNAM_ZONE);

    private final InvoiceReadFacade invoices;
    private final Environment environment;
    private final QrCodeSvgService qrCodeSvgService;

    @GetMapping("/student/payments/callback")
    public String providerReturn(@RequestParam(required = false) String invoiceId) {
        try {
            UUID parsed = UUID.fromString(invoiceId);
            return "redirect:/student/payment-result/" + parsed;
        } catch (IllegalArgumentException | NullPointerException invalidId) {
            return "redirect:/student/packages";
        }
    }

    @GetMapping("/student/checkout/{invoiceId}")
    public String checkout(@PathVariable UUID invoiceId, @AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "packages");
        model.addAttribute("invoiceId", invoiceId);
        model.addAttribute("paymentUnavailable", false);
        try {
            InvoiceSnapshot invoice = ownedInvoice(invoiceId, user.id());
            if ("PAID".equals(invoice.status())) {
                return "redirect:/student/payment-result/" + invoiceId;
            }
            model.addAttribute("invoice", toView(invoice));
            return "student/checkout";
        } catch (ResponseStatusException notFound) {
            throw notFound;
        } catch (RuntimeException ex) {
            log.warn("Student checkout page is unavailable", ex);
            model.addAttribute("paymentUnavailable", true);
            return "student/checkout";
        }
    }

    @GetMapping("/student/payment-result/{invoiceId}")
    public String result(@PathVariable UUID invoiceId, @AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "packages");
        model.addAttribute("invoiceId", invoiceId);
        model.addAttribute("paymentUnavailable", false);
        try {
            model.addAttribute("invoice", toView(ownedInvoice(invoiceId, user.id())));
        } catch (ResponseStatusException notFound) {
            throw notFound;
        } catch (RuntimeException ex) {
            log.warn("Student payment result is unavailable", ex);
            model.addAttribute("paymentUnavailable", true);
        }
        return "student/payment-result";
    }

    private InvoiceSnapshot ownedInvoice(UUID invoiceId, UUID studentId) {
        return invoices.findOwned(invoiceId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private InvoiceView toView(InvoiceSnapshot invoice) {
        String expiry = invoice.paymentExpiredAt() == null ? null : DATE_TIME.format(invoice.paymentExpiredAt());
        String status = switch (invoice.status() == null ? "" : invoice.status()) {
            case "PENDING", "PAID", "EXPIRED", "CANCELLED" -> invoice.status();
            default -> "UNKNOWN";
        };
        String checkoutUrl = safePayOsUrl(invoice.checkoutUrl());
        String qrPayload = (invoice.qrCode() != null && !invoice.qrCode().isBlank())
                ? invoice.qrCode()
                : checkoutUrl;
        String qrCodeDataUri = (qrPayload != null && !qrPayload.isBlank())
                ? qrCodeSvgService.generateSvgDataUri(qrPayload, 260)
                : null;

        return new InvoiceView(invoice.id(), invoice.invoiceNumber(),
                NumberFormat.getNumberInstance(VIETNAMESE).format(invoice.amountVnd()) + " ₫",
                status, checkoutUrl, expiry, qrCodeDataUri);
    }

    private String safePayOsUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443) && host != null
                    && ("payos.vn".equalsIgnoreCase(host) || host.toLowerCase(Locale.ROOT).endsWith(".payos.vn")
                    || (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("test"))
                    && "fake".equalsIgnoreCase(environment.getProperty("app.payment.provider", "disabled"))
                    && "pay.test".equalsIgnoreCase(host)))) {
                return uri.toASCIIString();
            }
        } catch (IllegalArgumentException ignored) {
            // Do not render malformed provider URLs as clickable links.
        }
        return null;
    }

    public record InvoiceView(UUID id, String invoiceNumber, String amountLabel, String status,
                              String checkoutUrl, String expiryLabel, String qrCodeDataUri) {}
}
