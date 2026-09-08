package com.edtech.platform.auth.security;

import com.edtech.platform.auth.service.OAuthAuthorizationPort;
import com.edtech.platform.auth.service.OAuthAuthorizationResult;
import com.edtech.platform.auth.service.OAuthIdentity;
import com.edtech.platform.common.config.properties.OAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final OAuthAuthorizationPort accounts;
    private final String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(OAuthAuthorizationPort accounts, OAuthProperties properties) {
        this.accounts = accounts;
        this.frontendRedirectUri = properties.frontendRedirectUri().toString();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        OAuthAuthorizationResult result = accounts.authorize(new OAuthIdentity(
                "GOOGLE", principal.getName(), principal.getAttribute("email"), principal.getAttribute("name")));

        String redirect;
        if (result instanceof OAuthAuthorizationResult.LoginExchange login) {
            log.info("Google OAuth login authorized");
            redirect = redirectWith("exchangeCode", login.exchangeCode());
        } else if (result instanceof OAuthAuthorizationResult.RegistrationRequired registration) {
            log.info("Google OAuth registration requires role selection");
            redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("registrationToken", registration.registrationToken())
                    .queryParam("needsRole", true)
                    .build().encode().toUriString();
        } else {
            var rejected = (OAuthAuthorizationResult.Rejected) result;
            log.warn("Google OAuth account authorization rejected code={}", rejected.errorCode());
            redirect = redirectWith("error", rejected.errorCode());
        }
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }

    private String redirectWith(String name, Object value) {
        return UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam(name, value)
                .build().encode().toUriString();
    }
}
