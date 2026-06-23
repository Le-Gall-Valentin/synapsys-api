package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.domain.model.DeleteAgentCommand;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAgentHandlerTest {

    @Mock AgentRepository agentRepository;
    private DeleteAgentHandler handler;
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() { handler = new DeleteAgentHandler(agentRepository); }

    private Agent agent(AgentLifecycleStatus status) {
        return new Agent(agentId, "web-01", new byte[32], "fp", status, UUID.randomUUID(),
            Instant.now(), UUID.randomUUID(), null, null, null,
            status == AgentLifecycleStatus.REVOKED ? Instant.now() : null, null);
    }

    @Test
    void delete_revokedAgent_succeeds() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.REVOKED)));
        when(agentRepository.delete(agentId)).thenReturn(true);
        assertThatCode(() -> handler.delete(new DeleteAgentCommand(agentId))).doesNotThrowAnyException();
        verify(agentRepository).delete(agentId);
    }

    @Test
    void delete_enrolledAgent_throwsAgentNotDeletable() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.ENROLLED)));
        assertThatThrownBy(() -> handler.delete(new DeleteAgentCommand(agentId)))
            .isInstanceOf(AgentException.AgentNotDeletable.class);
        verify(agentRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsAgentNotFound() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.delete(new DeleteAgentCommand(agentId)))
            .isInstanceOf(AgentException.AgentNotFound.class);
    }
}
