package com.edtech.platform.auth.web;

import com.edtech.platform.auth.dto.request.LoginRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.common.security.HtmlSessionState;
import com.edtech.platform.common.security.HtmlSessionLoginSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
public class HtmlLoginController {
    private final SessionService sessions;
    private final HtmlSessionLoginSupport sessionLogin;

    public HtmlLoginController(SessionService sessions, HtmlSessionLoginSupport sessionLogin) {
        this.sessions = sessions;
        this.sessionLogin = sessionLogin;
    }

    @GetMapping("/auth/login")
    public String loginPage(@RequestParam(required = false) String continueTo,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String oauthError,
                            Model model) {
        if (!model.containsAttribute("loginForm")) model.addAttribute("loginForm", new LoginForm("", ""));
        model.addAttribute("continueTo", safeLocalPath(continueTo));
        if (error != null && !model.containsAttribute("loginError")) {
            model.addAttribute("loginError", "Thông tin đăng nhập chưa chính xác. Vui lòng thử lại.");
        }
        if (oauthError != null && !model.containsAttribute("loginError")) {
            model.addAttribute("loginError", "Không thể tiếp tục bằng Google. Hãy thử lại hoặc đăng nhập bằng email.");
        }
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult errors,
                        @RequestParam(required = false) String continueTo,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        String safeContinueTo = safeLocalPath(continueTo);
        if (errors.hasErrors()) {
            errors.recordFieldValue("password", String.class, "");
            model.addAttribute("loginForm", new LoginForm(form.email(), ""));
            model.addAttribute("continueTo", safeContinueTo);
            model.addAttribute("loginError", "Vui lòng kiểm tra email và mật khẩu.");
            return "auth/login";
        }

        try {
            AuthResult result = sessions.login(new LoginRequest(form.email().trim(), form.password(), deviceInfo(request)),
                    request.getRemoteAddr());
            return "redirect:" + sessionLogin.establish(result, request, response, safeContinueTo);
        } catch (BusinessException failure) {
            errors.recordFieldValue("password", String.class, "");
            model.addAttribute("loginForm", new LoginForm(form.email(), ""));
            model.addAttribute("continueTo", safeContinueTo);
            String message = failure.getErrorCode() == com.edtech.platform.common.exception.ErrorCode.AUTH_INVALID_CREDENTIALS
                    ? "Email hoặc mật khẩu không chính xác."
                    : failure.getErrorCode().getDefaultMessage();
            model.addAttribute("loginError", message);
            return "auth/login";
        }
    }

    @PostMapping("/auth/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object value = session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE);
            if (value instanceof HtmlSessionState state) {
                try {
                    sessions.logout(state.refreshToken());
                } catch (RuntimeException ignored) {
                    // Revoke best-effort; always clear the browser session.
                }
            }
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        redirectAttributes.addFlashAttribute("logoutNotice", "Bạn đã đăng xuất.");
        return "redirect:/auth/login";
    }

    private String safeLocalPath(String value) {
        if (value == null || value.isBlank() || !value.startsWith("/") || value.startsWith("//")
                || value.contains("\\") || value.contains("\r") || value.contains("\n")) return null;
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawPath() == null
                    || uri.getRawPath().startsWith("/auth")) return null;
            return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private String deviceInfo(HttpServletRequest request) {
        String agent = request.getHeader("User-Agent");
        return agent == null ? "Tutor Match web" : agent.substring(0, Math.min(agent.length(), 200));
    }

    public record LoginForm(
            @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email,
            @NotBlank(message = "Mật khẩu không được để trống") String password) {}
}
