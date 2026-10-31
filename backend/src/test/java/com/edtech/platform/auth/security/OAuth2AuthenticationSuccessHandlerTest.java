package com.edtech.platform.auth.security;

import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.auth.service.OAuthAuthorizationResult;
import com.edtech.platform.auth.service.OAuthIdentity;
import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.OAuthExchangeRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.web.HtmlOAuthController;
import com.edtech.platform.common.config.properties.OAuthProperties;
import com.edtech.platform.common.security.HtmlSessionLoginSupport;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.common.security.HtmlSessionState;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2AuthenticationSuccessHandlerTest {
    private final OAuthAccountService accounts = mock(OAuthAccountService.class);
    private final OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
            accounts, new OAuthProperties(URI.create("http://localhost:3000/oauth2/callback")),
            new HtmlSessionLoginSupport(Clock.systemUTC()));

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

    @Test
    void htmlLoginExchangesCodeOnServerAndStoresOnlySessionState() throws Exception {
        when(accounts.authorize(any(OAuthIdentity.class)))
                .thenReturn(new OAuthAuthorizationResult.LoginExchange("exchange-code"));
        AuthResult auth = new AuthResult("access-server-token", "refresh-server-token", "Bearer", 900,
                new AuthResult.UserSummary(UUID.randomUUID(), "student@example.test", "Student", Role.STUDENT, UserStatus.ACTIVE, null));
        when(accounts.exchange(new OAuthExchangeRequest("exchange-code"), "127.0.0.1")).thenReturn(auth);

        var request = htmlRequest();
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo("/student");
        assertThat(request.getSession(false).getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE))
                .isInstanceOf(HtmlSessionState.class);
        assertThat(response.getRedirectedUrl()).doesNotContain("exchange-code", "access-server-token", "refresh-server-token");
    }

    @Test
    void htmlNewAccountKeepsRegistrationTokenInSession() throws Exception {
        when(accounts.authorize(any(OAuthIdentity.class)))
                .thenReturn(new OAuthAuthorizationResult.RegistrationRequired("registration-token"));
        var request = htmlRequest();
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/oauth/role");
        assertThat(request.getSession(false).getAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE))
                .isEqualTo("registration-token");
        assertThat(response.getRedirectedUrl()).doesNotContain("registration-token");
    }

    private MockHttpServletRequest htmlRequest() {
        var request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        request.getSession(true).setAttribute(HtmlOAuthController.HTML_FLOW_ATTRIBUTE, true);
        return request;
    }

    private Authentication authentication() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getName()).thenReturn("google-subject");
        when(principal.getAttribute("email")).thenReturn("student@example.test");
        when(principal.getAttribute("name")).thenReturn("Student");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
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
