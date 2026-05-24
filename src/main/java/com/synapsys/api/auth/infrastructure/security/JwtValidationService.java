package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtValidationService {

    private final SecretKey key;

    public JwtValidationService(SynapsysProperties properties) {
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
    }

    public UserClaims validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!JwtService.ISSUER.equals(claims.getIssuer())) {
                throw new JwtException("Invalid issuer: " + claims.getIssuer());
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(JwtService.AUDIENCE)) {
                throw new JwtException("Invalid audience");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            return new UserClaims(userId, role);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}