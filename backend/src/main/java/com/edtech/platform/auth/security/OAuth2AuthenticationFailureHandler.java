package com.edtech.platform.auth.security;

import com.edtech.platform.common.config.properties.OAuthProperties;
import com.edtech.platform.auth.web.HtmlOAuthController;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);
    private final String frontendRedirectUri;

    public OAuth2AuthenticationFailureHandler(OAuthProperties properties) {
        this.frontendRedirectUri = properties.frontendRedirectUri().toString();
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        log.warn("Google OAuth failed: type={}, path={}",
                exception.getClass().getSimpleName(), request.getRequestURI());

        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(HtmlOAuthController.HTML_FLOW_ATTRIBUTE))) {
            session.removeAttribute(HtmlOAuthController.HTML_FLOW_ATTRIBUTE);
            session.removeAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE);
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            SecurityContextHolder.clearContext();
            getRedirectStrategy().sendRedirect(request, response, "/auth/login?oauthError=1");
            return;
        }

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", "AUTH_OAUTH_PROVIDER_ERROR")
                .build()
                .encode()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
