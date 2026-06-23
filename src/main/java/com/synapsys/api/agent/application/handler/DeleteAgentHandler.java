package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.DeleteAgentUseCase;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.DeleteAgentCommand;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class DeleteAgentHandler implements DeleteAgentUseCase {

    private final AgentRepository agentRepository;

    public DeleteAgentHandler(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    @Transactional
    public void delete(DeleteAgentCommand command) {
        Agent agent = agentRepository.findById(command.agentId())
            .orElseThrow(AgentException.AgentNotFound::new);
        agent.ensureDeletable();
        if (!agentRepository.delete(command.agentId())) {
            throw new AgentException.AgentNotDeletable();
        }
    }
}
