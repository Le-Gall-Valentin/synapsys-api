package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.domain.model.RevokeAgentCommand;
import com.synapsys.api.agent.domain.port.out.AgentConnectionRegistryPort;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeAgentHandlerTest {

    @Mock AgentRepository agentRepository;
    @Mock AgentPresencePort presence;
    @Mock AgentConnectionRegistryPort connectionRegistry;

    private RevokeAgentHandler handler;
    private final UUID agentId = UUID.randomUUID();
    private final UUID caller = UUID.randomUUID();

    @BeforeEach
    void setUp() { handler = new RevokeAgentHandler(agentRepository, presence, connectionRegistry); }

    private Agent agent(AgentLifecycleStatus status) {
        return new Agent(agentId, "web-01", new byte[32], "fp", status, UUID.randomUUID(),
            Instant.now(), UUID.randomUUID(), null, null, null,
            status == AgentLifecycleStatus.REVOKED ? Instant.now() : null, null);
    }

    @Test
    void revoke_enrolledAgent_revokesClearsPresenceAndRequestsClose() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.ENROLLED)));
        when(agentRepository.markRevoked(eq(agentId), eq(caller), any())).thenReturn(true);
        assertThatCode(() -> handler.revoke(new RevokeAgentCommand(agentId, caller))).doesNotThrowAnyException();
        verify(presence).clear(agentId);
        verify(connectionRegistry).requestClose(agentId);
    }

    @Test
    void revoke_notFound_throwsAgentNotFound() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.revoke(new RevokeAgentCommand(agentId, caller)))
            .isInstanceOf(AgentException.AgentNotFound.class);
        verify(agentRepository, never()).markRevoked(any(), any(), any());
    }

    @Test
    void revoke_alreadyRevoked_throwsAgentNotRevocable() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.REVOKED)));
        assertThatThrownBy(() -> handler.revoke(new RevokeAgentCommand(agentId, caller)))
            .isInstanceOf(AgentException.AgentNotRevocable.class);
        verify(presence, never()).clear(any());
        verify(connectionRegistry, never()).requestClose(any());
    }
}

