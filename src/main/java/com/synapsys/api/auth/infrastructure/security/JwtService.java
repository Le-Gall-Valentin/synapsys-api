package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.AccessTokenPort;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService implements AccessTokenPort {

    private final SecretKey key;
    private final int expiryMinutes;

    public JwtService(SecretKey jwtSecretKey, SynapsysProperties properties) {
        this.key = jwtSecretKey;
        this.expiryMinutes = properties.jwt().expiryMinutes();
    }

    static final String ISSUER = "synapsys-api";
    static final String AUDIENCE = "synapsys-api";

    @Override
    public String generate(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(ISSUER)
            .audience().add(AUDIENCE).and()
            .subject(user.id().toString())
            .claim("role", user.role().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expiryMinutes * 60L)))
            .signWith(key)
            .compact();
    }
}