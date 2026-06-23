package com.synapsys.api.agent.infrastructure.persistence.adapter;

import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.IssuedToken;
import com.synapsys.api.agent.domain.model.NewEnrollmentToken;
import com.synapsys.api.agent.domain.port.out.AgentTokenHashPort;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenIssuerPort;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class EnrollmentTokenGenerator implements EnrollmentTokenIssuerPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EnrollmentTokenRepository repository;
    private final AgentTokenHashPort hashPort;
    private final AgentProperties properties;

    public EnrollmentTokenGenerator(EnrollmentTokenRepository repository,
                                    AgentTokenHashPort hashPort,
                                    AgentProperties properties) {
        this.repository = repository;
        this.hashPort = hashPort;
        this.properties = properties;
    }

    @Override
    public IssuedToken issue(String serverName, Duration requestedTtl, UUID createdBy) {
        Duration max = Duration.ofHours(properties.enrollmentTokenValidityHours());
        Duration effective = requestedTtl == null ? max : requestedTtl;
        if (effective.isZero() || effective.isNegative() || effective.compareTo(max) > 0) {
            throw new AgentException.InvalidTokenTtl();
        }
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String raw = properties.tokenPrefix()
            + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plus(effective);
        EnrollmentToken saved = repository.save(
            new NewEnrollmentToken(serverName, hashPort.hash(raw), expiresAt, createdBy));
        return new IssuedToken(saved.id(), raw, saved.serverName(), saved.expiresAt(), saved.createdAt());
    }
}
