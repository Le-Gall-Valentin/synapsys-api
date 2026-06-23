package com.synapsys.api.agent.infrastructure.ws;

import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caps the number of concurrent agent WebSocket connections per client IP. The {@code /ws/agents}
 * endpoint is unauthenticated at the transport layer (agents authenticate in-band via the Ed25519
 * challenge), so without this guard a single peer could open an unbounded number of sockets and
 * exhaust server / Redis resources.
 */
@Component
public class AgentConnectionLimiter {

    private final ConcurrentMap<String, Integer> connectionsPerIp = new ConcurrentHashMap<>();
    private final int maxPerIp;

    public AgentConnectionLimiter(AgentProperties properties) {
        this.maxPerIp = properties.maxConnectionsPerIp();
    }

    /** Atomically reserves a slot for the IP; {@code false} when the per-IP cap is already reached. */
    public boolean tryAcquire(String ip) {
        boolean[] acquired = {false};
        connectionsPerIp.compute(ip, (key, current) -> {
            int count = current == null ? 0 : current;
            if (count < maxPerIp) {
                acquired[0] = true;
                return count + 1;
            }
            return count;
        });
        return acquired[0];
    }

    /** Releases a previously reserved slot, dropping the entry once it reaches zero. */
    public void release(String ip) {
        connectionsPerIp.computeIfPresent(ip, (key, current) -> current <= 1 ? null : current - 1);
    }
}