package com.edtech.platform.auth.web;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.dto.request.CompleteOAuthRegistrationRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.HtmlSessionLoginSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HtmlOAuthController {
    public static final String HTML_FLOW_ATTRIBUTE = HtmlOAuthController.class.getName() + ".HTML_FLOW";
    public static final String PENDING_REGISTRATION_ATTRIBUTE = HtmlOAuthController.class.getName() + ".PENDING_REGISTRATION";

    private final OAuthAccountService oauthAccounts;
    private final HtmlSessionLoginSupport htmlSessions;

    public HtmlOAuthController(OAuthAccountService oauthAccounts, HtmlSessionLoginSupport htmlSessions) {
        this.oauthAccounts = oauthAccounts;
        this.htmlSessions = htmlSessions;
    }

    @GetMapping("/auth/oauth/google")
    public String startGoogle(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(HTML_FLOW_ATTRIBUTE, true);
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/auth/oauth/role")
    public String rolePage(HttpServletRequest request, Model model) {
        model.addAttribute("oauthRoleReady", hasPendingRegistration(request));
        if (!model.containsAttribute("oauthRoleForm")) model.addAttribute("oauthRoleForm", new OAuthRoleForm(null));
        if (!hasPendingRegistration(request)) {
            model.addAttribute("oauthRoleError", "Phiên đăng ký Google không còn hiệu lực. Quay lại đăng nhập để thử lại.");
        }
        return "auth/oauth-role";
    }

    @PostMapping("/auth/oauth/role")
    public String completeGoogleRegistration(@Valid @ModelAttribute("oauthRoleForm") OAuthRoleForm form,
                                             BindingResult errors,
                                             HttpServletRequest request,
                                             HttpServletResponse response,
                                             Model model) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(PENDING_REGISTRATION_ATTRIBUTE);
        if (!(value instanceof String token) || token.isBlank()) {
            model.addAttribute("oauthRoleReady", false);
            model.addAttribute("oauthRoleError", "Phiên đăng ký Google không còn hiệu lực. Quay lại đăng nhập để thử lại.");
            return "auth/oauth-role";
        }
        if (errors.hasErrors()) {
            model.addAttribute("oauthRoleReady", true);
            return "auth/oauth-role";
        }
        try {
            AuthResult auth = oauthAccounts.completeRegistration(
                    new CompleteOAuthRegistrationRequest(token, form.role()), request.getRemoteAddr());
            session.removeAttribute(PENDING_REGISTRATION_ATTRIBUTE);
            return "redirect:" + htmlSessions.establish(auth, request, response, null);
        } catch (BusinessException failure) {
            session.removeAttribute(PENDING_REGISTRATION_ATTRIBUTE);
            model.addAttribute("oauthRoleReady", false);
            model.addAttribute("oauthRoleError", failure.getErrorCode().getDefaultMessage());
            return "auth/oauth-role";
        }
    }

    /** Compatibility path for the former client callback; provider callbacks stay on Spring's server endpoint. */
    @GetMapping("/oauth2/callback")
    public String formerClientCallback() {
        return "redirect:/auth/login";
    }

    private boolean hasPendingRegistration(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(PENDING_REGISTRATION_ATTRIBUTE) instanceof String token
                && !token.isBlank();
    }

    public record OAuthRoleForm(@NotNull(message = "Hãy chọn vai trò") Role role) {}
}
