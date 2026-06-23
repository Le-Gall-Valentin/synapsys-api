package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.GetAgentStatisticsUseCase;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentStatistics;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationService
public class GetAgentStatisticsHandler implements GetAgentStatisticsUseCase {

    private final AgentRepository agentRepository;
    private final AgentPresencePort presence;

    public GetAgentStatisticsHandler(AgentRepository agentRepository, AgentPresencePort presence) {
        this.agentRepository = agentRepository;
        this.presence = presence;
    }

    @Override
    // REPEATABLE_READ so the non-revoked listing and the revoked count read a single snapshot;
    // otherwise a concurrent revoke between the two queries yields inconsistent totals.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AgentStatistics statistics() {
        List<Agent> nonRevoked = agentRepository.findAllNonRevoked();
        Set<UUID> present = presence.presentAgentIds(nonRevoked.stream().map(Agent::id).toList());
        long active = 0;
        long inactive = 0;
        long pending = 0;
        for (Agent a : nonRevoked) {
            switch (a.deriveStatus(present.contains(a.id()))) {
                case ACTIVE -> active++;
                case INACTIVE -> inactive++;
                case PENDING -> pending++;
                case REVOKED -> { /* excluded by findAllNonRevoked */ }
            }
        }
        long revoked = agentRepository.countRevoked();
        return new AgentStatistics(active, inactive, pending, revoked, nonRevoked.size() + revoked);
    }
}
