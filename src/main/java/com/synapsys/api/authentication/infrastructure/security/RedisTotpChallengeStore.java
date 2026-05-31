package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.authentication.domain.port.out.TotpChallengeStorePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisTotpChallengeStore implements TotpChallengeStorePort {

    private static final String CHALLENGE_PREFIX = "totp:challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public RedisTotpChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createChallenge(UUID userId) {
        String challengeId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CHALLENGE_PREFIX + challengeId, userId.toString(), CHALLENGE_TTL);
        return challengeId;
    }

    @Override
    public Optional<UUID> resolveChallenge(String challengeId) {
        String value = redisTemplate.opsForValue().get(CHALLENGE_PREFIX + challengeId);
        if (value == null) return Optional.empty();
        return Optional.of(UUID.fromString(value));
    }

    @Override
    public void invalidateChallenge(String challengeId) {
        redisTemplate.delete(CHALLENGE_PREFIX + challengeId);
    }

    @Override
    public int incrementFailedAttempts(String challengeId) {
        String key = "totp:attempts:" + challengeId;
        Long count = redisTemplate.opsForValue().increment(key);
        long attempts = count != null ? count : 1L;
        if (attempts == 1) {
            redisTemplate.expire(key, CHALLENGE_TTL);
        }
        return (int) attempts;
    }
}