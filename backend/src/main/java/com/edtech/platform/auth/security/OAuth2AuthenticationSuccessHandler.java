package com.edtech.platform.auth.security;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.auth.service.RedisTokenService;
import com.edtech.platform.common.config.properties.OAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserRepository userRepository;
    private final RedisTokenService redisTokenService;
    private final String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                              RedisTokenService redisTokenService,
                                              OAuthProperties properties) {
        this.userRepository = userRepository;
        this.redisTokenService = redisTokenService;
        this.frontendRedirectUri = properties.frontendRedirectUri().toString();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String subject = oAuth2User.getName();
        String provider = "GOOGLE";

        if (email == null) {
            log.warn("Google OAuth rejected because provider response has no email");
            getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?error=AUTH_OAUTH_LINK_NOT_ALLOWED");
            return;
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (provider.equals(user.getOauthProvider()) && subject.equals(user.getOauthSubject())) {
                log.info("Google OAuth login succeeded for userId={}", user.getId());
                String exchangeCode = redisTokenService.issue(RedisTokenService.Purpose.OAUTH_LOGIN_EXCHANGE, user.getId().toString());
                getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?exchangeCode=" + exchangeCode);
            } else {
                log.warn("Google OAuth account link rejected for existing userId={}", user.getId());
                getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri + "?error=AUTH_OAUTH_LINK_NOT_ALLOWED");
            }
        } else {
            log.info("Google OAuth registration requires role selection");
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
