package com.synapsys.api.agent.infrastructure.redis;

import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class RedisAgentPresenceStore implements AgentPresencePort {

    private static final String PRESENCE_PREFIX = "agent:presence:";

    // Atomic compare-and-delete: only removes the key when its value still belongs to this node
    // (value layout: "{nodeId}|{ip}|{connectedAtMillis}"), so a stale node cannot clear presence
    // that the agent has re-established elsewhere.
    private static final RedisScript<Long> CLEAR_IF_OWNED = RedisScript.of("""
        local current = redis.call('GET', KEYS[1])
        if current and string.sub(current, 1, string.len(ARGV[1])) == ARGV[1] then
            return redis.call('DEL', KEYS[1])
        end
        return 0
        """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration presenceTtl;

    public RedisAgentPresenceStore(StringRedisTemplate redis, AgentProperties properties) {
        this.redis = redis;
        this.presenceTtl = Duration.ofSeconds(properties.presenceTtlSeconds());
    }

    @Override
    public void markPresent(UUID agentId, String nodeId, String ip, Instant connectedAt) {
        redis.opsForValue().set(key(agentId), nodeId + "|" + ip + "|" + connectedAt.toEpochMilli(), presenceTtl);
    }

    @Override
    public void clear(UUID agentId) {
        redis.delete(key(agentId));
    }

    @Override
    public boolean clearIfOwnedBy(UUID agentId, String nodeId) {
        Long removed = redis.execute(CLEAR_IF_OWNED, List.of(key(agentId)), nodeId + "|");
        return removed != null && removed > 0;
    }

    @Override
    public boolean isPresent(UUID agentId) {
        return Boolean.TRUE.equals(redis.hasKey(key(agentId)));
    }

    @Override
    public Set<UUID> presentAgentIds(Collection<UUID> candidates) {
        if (candidates.isEmpty()) return Set.of();
        List<UUID> ordered = List.copyOf(candidates);
        List<String> keys = ordered.stream().map(this::key).toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        Set<UUID> present = new HashSet<>();
        if (values != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (i < values.size() && values.get(i) != null) {
                    present.add(ordered.get(i));
                }
            }
        }
        return present;
    }

    private String key(UUID agentId) {
        return PRESENCE_PREFIX + agentId;
    }
}
