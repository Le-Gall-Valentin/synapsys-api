package com.synapsys.api.agent.infrastructure.redis;

import com.synapsys.api.agent.domain.port.out.AgentChallengeStorePort;
import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Component
public class RedisAgentChallengeStore implements AgentChallengeStorePort {

    private static final String CHALLENGE_PREFIX = "agent:challenge:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final Duration challengeTtl;

    public RedisAgentChallengeStore(StringRedisTemplate redis, AgentProperties properties) {
        this.redis = redis;
        this.challengeTtl = Duration.ofSeconds(properties.challengeTtlSeconds());
    }

    @Override
    public String issueChallenge(String connectionId) {
        byte[] nonce = new byte[32];
        SECURE_RANDOM.nextBytes(nonce);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        redis.opsForValue().set(CHALLENGE_PREFIX + connectionId, value, challengeTtl);
        return value;
    }

    @Override
    public Optional<String> consumeChallenge(String connectionId) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(CHALLENGE_PREFIX + connectionId));
    }
}
