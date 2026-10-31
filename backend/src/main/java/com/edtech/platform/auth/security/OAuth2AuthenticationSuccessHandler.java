package com.edtech.platform.auth.security;

import com.edtech.platform.auth.service.OAuthAuthorizationResult;
import com.edtech.platform.auth.service.OAuthIdentity;
import com.edtech.platform.auth.dto.request.OAuthExchangeRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.auth.web.HtmlOAuthController;
import com.edtech.platform.common.config.properties.OAuthProperties;
import com.edtech.platform.common.security.HtmlSessionLoginSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final OAuthAccountService accounts;
    private final HtmlSessionLoginSupport htmlSessions;
    private final String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(OAuthAccountService accounts, OAuthProperties properties,
                                              HtmlSessionLoginSupport htmlSessions) {
        this.accounts = accounts;
        this.frontendRedirectUri = properties.frontendRedirectUri().toString();
        this.htmlSessions = htmlSessions;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        OAuthAuthorizationResult result = accounts.authorize(new OAuthIdentity(
                "GOOGLE", principal.getName(), principal.getAttribute("email"), principal.getAttribute("name")));
        boolean htmlFlow = isHtmlFlow(request);

        String redirect;
        if (result instanceof OAuthAuthorizationResult.LoginExchange login) {
            log.info("Google OAuth login authorized");
            if (htmlFlow) {
                AuthResult auth = accounts.exchange(new OAuthExchangeRequest(login.exchangeCode()), request.getRemoteAddr());
                clearHtmlOAuthState(request, false);
                redirect = htmlSessions.establish(auth, request, response, null);
            } else {
                redirect = redirectWith("exchangeCode", login.exchangeCode());
            }
        } else if (result instanceof OAuthAuthorizationResult.RegistrationRequired registration) {
            log.info("Google OAuth registration requires role selection");
            if (htmlFlow) {
                request.getSession(true).setAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE,
                        registration.registrationToken());
                clearHtmlOAuthState(request, true);
                redirect = "/auth/oauth/role";
            } else {
                redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                        .queryParam("registrationToken", registration.registrationToken())
                        .queryParam("needsRole", true)
                        .build().encode().toUriString();
            }
        } else {
            var rejected = (OAuthAuthorizationResult.Rejected) result;
            log.warn("Google OAuth account authorization rejected code={}", rejected.errorCode());
            if (htmlFlow) {
                clearHtmlOAuthState(request, true);
                redirect = "/auth/login?oauthError=1";
            } else {
                redirect = redirectWith("error", rejected.errorCode());
            }
        }
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }

    private boolean isHtmlFlow(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(HtmlOAuthController.HTML_FLOW_ATTRIBUTE));
    }

    private void clearHtmlOAuthState(HttpServletRequest request, boolean clearAuthentication) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(HtmlOAuthController.HTML_FLOW_ATTRIBUTE);
            if (clearAuthentication) session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }
        if (clearAuthentication) SecurityContextHolder.clearContext();
    }

    private String redirectWith(String name, Object value) {
        return UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam(name, value)
                .build().encode().toUriString();
    }
}
