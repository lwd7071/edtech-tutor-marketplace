package com.edtech.platform.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("role", user.role())
                .claim("email", user.email())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser getAuthenticatedUserFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
                
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class)
        );
    }
}
