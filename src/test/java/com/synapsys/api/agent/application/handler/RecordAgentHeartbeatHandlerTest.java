package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecordAgentHeartbeatHandlerTest {

    @Mock AgentPresencePort presence;

    private RecordAgentHeartbeatHandler handler;
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new RecordAgentHeartbeatHandler(presence);
    }

    @Test
    void heartbeat_reassertsPresenceForThisNode() {
        handler.heartbeat(agentId, "node-A", "1.2.3.4");

        // Re-asserts (upsert), not a bare TTL refresh, so presence self-heals and ownership is reclaimed.
        verify(presence).markPresent(eq(agentId), eq("node-A"), eq("1.2.3.4"), any(Instant.class));
    }
}
