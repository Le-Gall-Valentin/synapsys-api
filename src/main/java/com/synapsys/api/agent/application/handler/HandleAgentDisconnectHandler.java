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
    public void disconnect(UUID agentId, String ip) {
        agentRepository.updateActivitySnapshot(agentId, Instant.now(), ip);
        presence.clear(agentId);
    }
}
