package com.synapsys.api.agent.infrastructure.persistence.adapter;

import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.NewEnrollmentToken;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class EnrollmentTokenRepositoryAdapter implements EnrollmentTokenRepository {

    private final EnrollmentTokenJpaRepository jpa;

    public EnrollmentTokenRepositoryAdapter(EnrollmentTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public EnrollmentToken save(NewEnrollmentToken newToken) {
        EnrollmentTokenEntity e = new EnrollmentTokenEntity(
            newToken.serverName(), newToken.tokenHash(), newToken.expiresAt(), newToken.createdBy());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<EnrollmentToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<EnrollmentToken> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean markConsumed(UUID id, Instant now) {
        return jpa.markConsumed(id, now) == 1;
    }

    @Override
    public boolean markRevoked(UUID id, UUID revokedBy, Instant now) {
        return jpa.markRevoked(id, revokedBy, now) == 1;
    }

    @Override
    public PageResult<EnrollmentToken> findAll(int page, int size, SortRequest sort) {
        Sort springSort = sort.ascending()
            ? Sort.by(sort.field()).ascending()
            : Sort.by(sort.field()).descending();
        Page<EnrollmentTokenEntity> result = jpa.findAll(PageRequest.of(page, size, springSort));
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(),
            result.getTotalElements(), page, size);
    }

    private EnrollmentToken toDomain(EnrollmentTokenEntity e) {
        return new EnrollmentToken(e.getId(), e.getServerName(), e.getConsumedAt(), e.getRevokedAt(),
            e.getRevokedBy(), e.getExpiresAt(), e.getCreatedAt(), e.getCreatedBy());
    }
}
