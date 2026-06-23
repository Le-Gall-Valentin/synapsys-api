package com.synapsys.api.agent.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentConnectionRegistryTest {

    @Mock StringRedisTemplate redis;

    @Test
    void requestClose_publishesAgentIdToRevokeChannel() {
        UUID agentId = UUID.randomUUID();
        new AgentConnectionRegistry(redis).requestClose(agentId);
        verify(redis).convertAndSend(AgentConnectionRegistry.REVOKE_CHANNEL, agentId.toString());
    }
}
