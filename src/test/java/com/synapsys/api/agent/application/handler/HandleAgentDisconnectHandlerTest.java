package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleAgentDisconnectHandlerTest {

    @Mock AgentRepository agentRepository;
    @Mock AgentPresencePort presence;

    private HandleAgentDisconnectHandler handler;
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new HandleAgentDisconnectHandler(agentRepository, presence);
    }

    @Test
    void disconnect_whenNodeOwnsPresence_clearsAndFlushesSnapshot() {
        when(presence.clearIfOwnedBy(agentId, "node-A")).thenReturn(true);

        handler.disconnect(agentId, "1.2.3.4", "node-A");

        verify(agentRepository).updateActivitySnapshot(eq(agentId), any(Instant.class), eq("1.2.3.4"));
    }

    @Test
    void disconnect_whenPresenceOwnedByAnotherNode_isNoOp() {
        // The agent already reconnected to another instance: this stale node must not touch state.
        when(presence.clearIfOwnedBy(agentId, "node-A")).thenReturn(false);

        handler.disconnect(agentId, "1.2.3.4", "node-A");

        verify(agentRepository, never()).updateActivitySnapshot(any(), any(), any());
    }
}
