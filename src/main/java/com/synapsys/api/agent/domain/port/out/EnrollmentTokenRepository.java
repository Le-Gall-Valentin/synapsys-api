package com.synapsys.api.agent.domain.port.out;

import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.NewEnrollmentToken;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentTokenRepository {
    EnrollmentToken save(NewEnrollmentToken newToken);
    Optional<EnrollmentToken> findByTokenHash(String tokenHash);
    Optional<EnrollmentToken> findById(UUID id);
    /** Atomic guard: sets consumed_at only if still ACTIVE at {@code now}; true if exactly one row changed. */
    boolean markConsumed(UUID id, Instant now);
    /** Atomic guard: sets revoked_at/revoked_by only if still ACTIVE at {@code now}; true if exactly one row changed. */
    boolean markRevoked(UUID id, UUID revokedBy, Instant now);
    PageResult<EnrollmentToken> findAll(int page, int size, SortRequest sort);
}
