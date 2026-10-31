package com.edtech.platform.auth.web;

import com.edtech.platform.auth.dto.request.ForgotPasswordRequest;
import com.edtech.platform.auth.dto.request.ResetPasswordRequest;
import com.edtech.platform.auth.dto.request.VerifyEmailRequest;
import com.edtech.platform.auth.service.AccountVerificationService;
import com.edtech.platform.auth.service.PasswordRecoveryService;
import com.edtech.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HtmlAccountAccessController {
    private static final String EMAIL_VERIFY_TOKEN = HtmlAccountAccessController.class.getName() + ".EMAIL_VERIFY_TOKEN";
    private static final String PASSWORD_RESET_TOKEN = HtmlAccountAccessController.class.getName() + ".PASSWORD_RESET_TOKEN";

    private final AccountVerificationService verification;
    private final PasswordRecoveryService recovery;

    public HtmlAccountAccessController(AccountVerificationService verification, PasswordRecoveryService recovery) {
        this.verification = verification;
        this.recovery = recovery;
    }

    @GetMapping("/auth/verify-email")
    public String verifyPage(@RequestParam(required = false) String token,
                             HttpServletRequest request,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (token != null && !token.isBlank()) {
            if (token.length() > 512) {
                redirectAttributes.addFlashAttribute("verificationError", "Liên kết xác minh không hợp lệ hoặc đã hết hạn.");
                return "redirect:/auth/verify-email";
            } else {
                HttpSession session = request.getSession(true);
                request.changeSessionId();
                session.setAttribute(EMAIL_VERIFY_TOKEN, token);
                return "redirect:/auth/verify-email";
            }
        }
        model.addAttribute("verificationReady", hasToken(request, EMAIL_VERIFY_TOKEN));
        if (!model.containsAttribute("emailForm")) model.addAttribute("emailForm", new ForgotPasswordRequest(""));
        return "auth/verify-email";
    }

    @PostMapping("/auth/verify-email")
    public String verify(HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(EMAIL_VERIFY_TOKEN);
        if (!(value instanceof String token) || token.isBlank()) {
            model.addAttribute("verificationError", "Hãy mở liên kết xác minh trong email trước khi tiếp tục.");
            model.addAttribute("verificationReady", false);
            model.addAttribute("emailForm", new ForgotPasswordRequest(""));
            return "auth/verify-email";
        }
        try {
            verification.verify(new VerifyEmailRequest(token));
            session.removeAttribute(EMAIL_VERIFY_TOKEN);
            redirectAttributes.addFlashAttribute("verificationComplete", true);
            return "redirect:/auth/verify-email";
        } catch (BusinessException invalid) {
            session.removeAttribute(EMAIL_VERIFY_TOKEN);
            redirectAttributes.addFlashAttribute("verificationError", "Liên kết xác minh không hợp lệ hoặc đã hết hạn.");
            return "redirect:/auth/verify-email";
        }
    }

    @PostMapping("/auth/verify-email/resend")
    public String resendVerification(@Valid @ModelAttribute("emailForm") ForgotPasswordRequest form,
                                     BindingResult errors,
                                     HttpServletRequest request,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("verificationReady", hasToken(request, EMAIL_VERIFY_TOKEN));
            return "auth/verify-email";
        }
        try {
            verification.resend(form, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("resendNotice", "Nếu email cần xác minh, hướng dẫn đã được gửi.");
            return "redirect:/auth/verify-email";
        } catch (BusinessException limited) {
            model.addAttribute("verificationReady", hasToken(request, EMAIL_VERIFY_TOKEN));
            model.addAttribute("resendError", limited.getErrorCode().getDefaultMessage());
            return "auth/verify-email";
        }
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPage(@RequestParam(required = false) String sent, Model model) {
        if (!model.containsAttribute("emailForm")) model.addAttribute("emailForm", new ForgotPasswordRequest(""));
        model.addAttribute("requestSent", sent != null);
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String requestReset(@Valid @ModelAttribute("emailForm") ForgotPasswordRequest form,
                               BindingResult errors,
                               HttpServletRequest request,
                               Model model) {
        if (errors.hasErrors()) return "auth/forgot-password";
        try {
            recovery.requestReset(form, request.getRemoteAddr());
            return "redirect:/auth/forgot-password?sent";
        } catch (BusinessException limited) {
            model.addAttribute("requestError", limited.getErrorCode().getDefaultMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/auth/reset-password")
    public String resetPage(@RequestParam(required = false) String token,
                            HttpServletRequest request,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (token != null && !token.isBlank()) {
            if (token.length() > 512) {
                redirectAttributes.addFlashAttribute("resetError", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
                return "redirect:/auth/reset-password";
            } else {
                HttpSession session = request.getSession(true);
                request.changeSessionId();
                session.setAttribute(PASSWORD_RESET_TOKEN, token);
                return "redirect:/auth/reset-password";
            }
        }
        model.addAttribute("resetTokenReady", hasToken(request, PASSWORD_RESET_TOKEN));
        if (!model.containsAttribute("resetForm")) model.addAttribute("resetForm", new ResetPasswordForm(""));
        return "auth/reset-password";
    }

    @PostMapping("/auth/reset-password")
    public String reset(@Valid @ModelAttribute("resetForm") ResetPasswordForm form,
                        BindingResult errors,
                        HttpServletRequest request,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(PASSWORD_RESET_TOKEN);
        if (!(value instanceof String token) || token.isBlank()) {
            model.addAttribute("resetTokenReady", false);
            model.addAttribute("resetError", "Hãy mở liên kết đặt lại mật khẩu trong email trước khi tiếp tục.");
            return "auth/reset-password";
        }
        if (errors.hasErrors()) {
            errors.recordFieldValue("newPassword", String.class, "");
            model.addAttribute("resetTokenReady", true);
            return "auth/reset-password";
        }
        try {
            recovery.reset(new ResetPasswordRequest(token, form.newPassword()));
            session.removeAttribute(PASSWORD_RESET_TOKEN);
            redirectAttributes.addFlashAttribute("passwordResetComplete", true);
            return "redirect:/auth/login";
        } catch (BusinessException invalid) {
            errors.recordFieldValue("newPassword", String.class, "");
            session.removeAttribute(PASSWORD_RESET_TOKEN);
            model.addAttribute("resetTokenReady", false);
            model.addAttribute("resetError", invalid.getErrorCode().getDefaultMessage());
            return "auth/reset-password";
        }
    }

    private boolean hasToken(HttpServletRequest request, String name) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(name) instanceof String token && !token.isBlank();
    }

    public record ResetPasswordForm(
            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                    message = "Mật khẩu phải dài ít nhất 8 ký tự, gồm chữ cái, chữ số và ký tự đặc biệt")
            String newPassword) {}
}
