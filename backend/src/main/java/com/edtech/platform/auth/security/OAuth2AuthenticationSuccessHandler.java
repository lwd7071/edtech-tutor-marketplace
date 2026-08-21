package com.edtech.platform.auth.security;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.auth.service.RedisTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RedisTokenService redisTokenService;
    private final String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                              RedisTokenService redisTokenService,
                                              @Value("${app.oauth2.frontend-redirect-uri:http://localhost:3000/oauth2/callback}") String frontendRedirectUri) {
        this.userRepository = userRepository;
        this.redisTokenService = redisTokenService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String subject = oAuth2User.getName();
        String provider = "GOOGLE";

        if (email == null) {
            getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?error=AUTH_OAUTH_LINK_NOT_ALLOWED");
            return;
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (provider.equals(user.getOauthProvider()) && subject.equals(user.getOauthSubject())) {
                String exchangeCode = redisTokenService.issue(RedisTokenService.Purpose.OAUTH_LOGIN_EXCHANGE, user.getId().toString());
                getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?exchangeCode=" + exchangeCode);
            } else {
                getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?error=AUTH_OAUTH_LINK_NOT_ALLOWED");
            }
        } else {
            Map<String, String> payload = Map.of(
                    "email", email,
                    "oauthProvider", provider,
                    "oauthSubject", subject,
                    "fullName", name != null ? name : ""
            );
            String registrationToken = redisTokenService.issue(RedisTokenService.Purpose.OAUTH_REGISTRATION, payload);
            getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?registrationToken=" + registrationToken + "&needsRole=true");
        }
    }
}
