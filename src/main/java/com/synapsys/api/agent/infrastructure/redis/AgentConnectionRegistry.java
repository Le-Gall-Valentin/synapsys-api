package com.synapsys.api.agent.infrastructure.redis;

import com.synapsys.api.agent.domain.port.out.AgentConnectionRegistryPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AgentConnectionRegistry implements AgentConnectionRegistryPort {

    public static final String REVOKE_CHANNEL = "agent:revoke";

    private final StringRedisTemplate redis;

    public AgentConnectionRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void requestClose(UUID agentId) {
        // Fan-out to every API node; the node holding the live session (Layer 4 subscriber) closes it.
        redis.convertAndSend(REVOKE_CHANNEL, agentId.toString());
    }
}
