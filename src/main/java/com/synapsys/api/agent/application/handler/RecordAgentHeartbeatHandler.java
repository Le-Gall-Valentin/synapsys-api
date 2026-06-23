package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.RecordAgentHeartbeatUseCase;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.shared.annotation.ApplicationService;

import java.util.UUID;

@ApplicationService
public class RecordAgentHeartbeatHandler implements RecordAgentHeartbeatUseCase {

    private final AgentPresencePort presence;

    public RecordAgentHeartbeatHandler(AgentPresencePort presence) {
        this.presence = presence;
    }

    @Override
    public void heartbeat(UUID agentId) {
        presence.refresh(agentId);
    }
}
