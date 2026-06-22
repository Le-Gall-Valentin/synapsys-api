package com.synapsys.api.agent.domain.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface AgentPresencePort {
    void markPresent(UUID agentId, String nodeId, String ip, Instant connectedAt);
    void refresh(UUID agentId);
    void clear(UUID agentId);
    boolean isPresent(UUID agentId);
    Set<UUID> presentAgentIds(Collection<UUID> candidates);
}
