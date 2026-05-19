package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.RefreshTokenIssuerPort;
import com.synapsys.api.auth.domain.port.out.TokenHashPort;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public class RefreshTokenGenerator implements RefreshTokenIssuerPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenJpaRepository jpa;
    private final TokenHashPort tokenHashPort;

    public RefreshTokenGenerator(RefreshTokenJpaRepository jpa, TokenHashPort tokenHashPort) {
        this.jpa = jpa;
        this.tokenHashPort = tokenHashPort;
    }

    @Override
    public String generate(User user, int expiryDays) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(user.id());
        entity.setTokenHash(tokenHashPort.hash(raw));
        entity.setExpiresAt(Instant.now().plusSeconds((long) expiryDays * 86400));
        jpa.save(entity);
        return raw;
    }
}