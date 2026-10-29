package com.edtech.platform.auth.web;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.dto.request.RegisterRequest;
import com.edtech.platform.auth.dto.response.RegistrationResult;
import com.edtech.platform.auth.service.RegistrationService;
import com.edtech.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HtmlRegistrationController {
    private final RegistrationService registrations;

    public HtmlRegistrationController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping("/auth/register")
    public String registerPage(@RequestParam(name = "role", required = false) String requestedRole, Model model) {
        if (!model.containsAttribute("registerForm")) {
            Role role = Role.TEACHER.name().equals(requestedRole) ? Role.TEACHER : Role.STUDENT;
            model.addAttribute("registerForm", new RegisterRequest("", "", "", role, "", "", ""));
        }
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterRequest form,
                           BindingResult errors,
                           HttpServletRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            clearPassword(errors);
            return "auth/register";
        }

        try {
            RegistrationResult result = registrations.register(form, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("registered", result.verificationRequired());
            return "redirect:/auth/verify-email";
        } catch (BusinessException failure) {
            clearPassword(errors);
            model.addAttribute("registerError", failure.getMessage());
            return "auth/register";
        }
    }

    private void clearPassword(BindingResult errors) {
        errors.recordFieldValue("password", String.class, "");
    }
}
