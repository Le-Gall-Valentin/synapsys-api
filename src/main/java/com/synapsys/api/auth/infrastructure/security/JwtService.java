package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.AccessTokenPort;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService implements AccessTokenPort {

    private final SecretKey key;
    private final int expiryMinutes;

    public JwtService(SynapsysProperties properties) {
        String secret = properties.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("synapsys.jwt.secret must be configured");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "synapsys.jwt.secret must be at least 32 characters long (got " + keyBytes.length + ")"
            );
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiryMinutes = properties.jwt().expiryMinutes();
    }

    @Override
    public String generate(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.id().toString())
            .claim("role", user.role().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expiryMinutes * 60L)))
            .signWith(key)
            .compact();
    }

    public UserClaims validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            return new UserClaims(userId, role);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}