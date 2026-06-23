package com.synapsys.api.agent.domain.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface AgentPresencePort {
    /** Upserts presence (node ownership + TTL). Called on connect and on every heartbeat so a live
     *  connection always re-asserts itself, even after a transient TTL expiry or a cross-node race. */
    void markPresent(UUID agentId, String nodeId, String ip, Instant connectedAt);
    /** Unconditionally clears presence (used by revocation). */
    void clear(UUID agentId);
    /** Clears presence only if it is still owned by {@code nodeId}; returns true if it was cleared.
     *  Lets a stale session on one node skip clearing presence the agent re-established on another node. */
    boolean clearIfOwnedBy(UUID agentId, String nodeId);
    boolean isPresent(UUID agentId);
    Set<UUID> presentAgentIds(Collection<UUID> candidates);
}
