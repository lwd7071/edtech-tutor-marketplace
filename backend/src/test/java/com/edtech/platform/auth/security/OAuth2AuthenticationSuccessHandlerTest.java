package com.edtech.platform.auth.security;

import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.auth.service.OAuthAuthorizationResult;
import com.edtech.platform.auth.service.OAuthIdentity;
import com.edtech.platform.common.config.properties.OAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2AuthenticationSuccessHandlerTest {
    private final OAuthAccountService accounts = mock(OAuthAccountService.class);
    private final OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
            accounts, new OAuthProperties(URI.create("http://localhost:3000/oauth2/callback")));

    @Test
    void redirectsExistingAccountWithOneTimeExchangeCode() throws Exception {
        when(accounts.authorize(any(OAuthIdentity.class)))
                .thenReturn(new OAuthAuthorizationResult.LoginExchange("exchange-code"));

        assertThat(authenticate()).isEqualTo(
                "http://localhost:3000/oauth2/callback?exchangeCode=exchange-code");
    }

    @Test
    void redirectsNewAccountToRoleSelection() throws Exception {
        when(accounts.authorize(any(OAuthIdentity.class)))
                .thenReturn(new OAuthAuthorizationResult.RegistrationRequired("registration-token"));

        assertThat(authenticate()).isEqualTo(
                "http://localhost:3000/oauth2/callback?registrationToken=registration-token&needsRole=true");
    }

    @Test
    void redirectsRejectedAccountWithStableErrorCode() throws Exception {
        when(accounts.authorize(any(OAuthIdentity.class)))
                .thenReturn(new OAuthAuthorizationResult.Rejected("AUTH_OAUTH_LINK_NOT_ALLOWED"));

        assertThat(authenticate()).isEqualTo(
                "http://localhost:3000/oauth2/callback?error=AUTH_OAUTH_LINK_NOT_ALLOWED");
    }

    private String authenticate() throws Exception {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getName()).thenReturn("google-subject");
        when(principal.getAttribute("email")).thenReturn("student@example.test");
        when(principal.getAttribute("name")).thenReturn("Student");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        var request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);
        return response.getRedirectedUrl();
    }
}
