package com.edtech.platform.auth.security;

import com.edtech.platform.common.config.properties.OAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

class OAuth2AuthenticationFailureHandlerTest {

    @Test
    void redirectsProviderFailureToFrontendWithStableErrorCode() throws Exception {
        var handler = new OAuth2AuthenticationFailureHandler(
                new OAuthProperties(URI.create("http://localhost:3000/oauth2/callback")));
        var request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        var response = new MockHttpServletResponse();
        var exception = new OAuth2AuthenticationException(new OAuth2Error("access_denied"));

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/oauth2/callback?error=AUTH_OAUTH_PROVIDER_ERROR");
    }
}
