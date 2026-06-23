package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.ListAgentsUseCase;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentView;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationService
public class ListAgentsHandler implements ListAgentsUseCase {

    private final AgentRepository agentRepository;
    private final AgentPresencePort presence;

    public ListAgentsHandler(AgentRepository agentRepository, AgentPresencePort presence) {
        this.agentRepository = agentRepository;
        this.presence = presence;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgentView> list(int page, int size, SortRequest sort) {
        PageResult<Agent> result = agentRepository.findAll(page, size, sort);
        Set<UUID> present = presence.presentAgentIds(result.content().stream().map(Agent::id).toList());
        List<AgentView> views = result.content().stream()
            .map(a -> new AgentView(a.id(), a.serverName(), a.ipAddress(),
                a.deriveStatus(present.contains(a.id())), a.fingerprint(), a.enrolledAt(), a.lastActivityAt()))
            .toList();
        return new PageResult<>(views, result.totalElements(), result.page(), result.size());
    }
}
