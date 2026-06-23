package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.RecordAgentHeartbeatUseCase;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.shared.annotation.ApplicationService;

import java.time.Instant;
import java.util.UUID;

@ApplicationService
public class RecordAgentHeartbeatHandler implements RecordAgentHeartbeatUseCase {

    private final AgentPresencePort presence;

    public RecordAgentHeartbeatHandler(AgentPresencePort presence) {
        this.presence = presence;
    }

    @Override
    public void heartbeat(UUID agentId, String nodeId, String ip) {
        // Re-assert presence (not just extend TTL): self-heals after a transient TTL expiry and
        // re-claims node ownership for the live connection, so reconnect churn never leaves an agent
        // stuck INACTIVE across instances.
        presence.markPresent(agentId, nodeId, ip, Instant.now());
    }
}
