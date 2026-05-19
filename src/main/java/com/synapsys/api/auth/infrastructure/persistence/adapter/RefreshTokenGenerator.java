package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.RefreshTokenPort;
import com.synapsys.api.auth.domain.port.out.TokenHashPort;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RefreshTokenGenerator implements RefreshTokenPort {

    private final RefreshTokenJpaRepository jpa;
    private final TokenHashPort tokenHashPort;

    public RefreshTokenGenerator(RefreshTokenJpaRepository jpa, TokenHashPort tokenHashPort) {
        this.jpa = jpa;
        this.tokenHashPort = tokenHashPort;
    }

    @Override
    public String generate(User user, int expiryDays) {
        String raw = UUID.randomUUID().toString();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(user.id());
        entity.setTokenHash(tokenHashPort.hash(raw));
        entity.setExpiresAt(Instant.now().plusSeconds((long) expiryDays * 86400));
        jpa.save(entity);
        return raw;
    }
}