package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.RevokeAgentUseCase;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.RevokeAgentCommand;
import com.synapsys.api.agent.domain.port.out.AgentConnectionRegistryPort;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@ApplicationService
public class RevokeAgentHandler implements RevokeAgentUseCase {

    private final AgentRepository agentRepository;
    private final AgentPresencePort presence;
    private final AgentConnectionRegistryPort connectionRegistry;

    public RevokeAgentHandler(AgentRepository agentRepository, AgentPresencePort presence,
                              AgentConnectionRegistryPort connectionRegistry) {
        this.agentRepository = agentRepository;
        this.presence = presence;
        this.connectionRegistry = connectionRegistry;
    }

    @Override
    @Transactional
    public void revoke(RevokeAgentCommand command) {
        Instant now = Instant.now();
        Agent agent = agentRepository.findById(command.agentId())
            .orElseThrow(AgentException.AgentNotFound::new);
        agent.ensureRevocable();
        if (!agentRepository.markRevoked(command.agentId(), command.callerId(), now)) {
            throw new AgentException.AgentNotRevocable();
        }
        // Run the Redis side effects only once the revocation is durably committed: a rolled-back
        // transaction must never leave a live connection closed / presence cleared for an agent
        // whose row is still ENROLLED (it would otherwise reconnect freely).
        runAfterCommit(() -> {
            presence.clear(command.agentId());
            connectionRegistry.requestClose(command.agentId());
        });
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            // No active transaction (e.g. unit tests): run inline.
            action.run();
        }
    }
}
