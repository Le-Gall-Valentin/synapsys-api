package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtValidationService {

    private final SecretKey key;

    public JwtValidationService(SynapsysProperties properties) {
        this.key = JwtKeyFactory.from(properties.jwt().secret());
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