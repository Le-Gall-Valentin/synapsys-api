package com.synapsys.api.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

@Component
class CaffeineAttemptTracker implements AttemptTracker {

    private final LongSupplier clock;

    // Single bounded cache — composite key encodes the window to avoid cross-window collisions.
    // TTL is a safe upper bound over the longest expected rate-limit window.
    private final Cache<String, Deque<Long>> cache = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    CaffeineAttemptTracker() {
        this(System::currentTimeMillis);
    }

    CaffeineAttemptTracker(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public boolean isLimitExceeded(String key, int max, int windowSeconds) {
        long windowMs = windowSeconds * 1_000L;
        String cacheKey = windowSeconds + ":" + key;
        long now = clock.getAsLong();
        boolean[] exceeded = {false};

        cache.asMap().compute(cacheKey, (k, existing) -> {
            Deque<Long> deque = existing != null ? existing : new ArrayDeque<>();
            while (!deque.isEmpty() && deque.peekFirst() < now - windowMs) {
                deque.pollFirst();
            }
            if (deque.size() >= max) {
                exceeded[0] = true;
            } else {
                deque.addLast(now);
            }
            return deque;
        });

        return exceeded[0];
    }
}
