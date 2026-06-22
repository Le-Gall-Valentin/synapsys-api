package com.synapsys.api.agent.domain.port.out;

import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.NewAgent;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository {
    /** Persists a new agent (status ENROLLED). Maps the public-key unique violation to PublicKeyAlreadyRegistered. */
    Agent insert(NewAgent newAgent);
    Optional<Agent> findById(UUID id);
    boolean existsByPublicKey(byte[] publicKey);
    /** Sets first_connected_at (only if null), last_activity_at and ip_address. */
    void markConnected(UUID id, Instant when, String ip);
    /** Flushes last_activity_at and ip_address (e.g. on disconnect). */
    void updateActivitySnapshot(UUID id, Instant lastActivityAt, String ip);
    /** Atomic guard: ENROLLED -> REVOKED; true if exactly one row changed. */
    boolean markRevoked(UUID id, UUID revokedBy, Instant now);
    /** Atomic guard: deletes only if status is REVOKED; true if exactly one row removed. */
    boolean delete(UUID id);
    PageResult<Agent> findAll(int page, int size, SortRequest sort);
    List<Agent> findAllNonRevoked();
    long countRevoked();
}
