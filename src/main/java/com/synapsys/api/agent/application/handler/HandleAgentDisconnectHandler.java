package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.HandleAgentDisconnectUseCase;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationService
public class HandleAgentDisconnectHandler implements HandleAgentDisconnectUseCase {

    private final AgentRepository agentRepository;
    private final AgentPresencePort presence;

    public HandleAgentDisconnectHandler(AgentRepository agentRepository, AgentPresencePort presence) {
        this.agentRepository = agentRepository;
        this.presence = presence;
    }

    @Override
    @Transactional
    public void disconnect(UUID agentId, String ip, String nodeId) {
        // Only the node that still owns the presence finalizes the disconnect. A stale session on
        // another node (the agent reconnected elsewhere) must not clear the live presence nor
        // overwrite the activity snapshot.
        if (presence.clearIfOwnedBy(agentId, nodeId)) {
            agentRepository.updateActivitySnapshot(agentId, Instant.now(), ip);
        }
    }
}
