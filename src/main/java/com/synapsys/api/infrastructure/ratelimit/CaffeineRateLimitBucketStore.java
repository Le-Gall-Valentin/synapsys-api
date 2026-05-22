package com.synapsys.api.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
class CaffeineRateLimitBucketStore implements RateLimitBucketStore {

    private final TimeMeter clock;
    private final Cache<String, Bucket> buckets;

    CaffeineRateLimitBucketStore() {
        this(TimeMeter.SYSTEM_NANOTIME);
    }

    CaffeineRateLimitBucketStore(TimeMeter clock) {
        this.clock = clock;
        this.buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    }

    @Override
    public BucketResult tryConsume(String key, int max, int windowSeconds) {
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
            .withCustomTimePrecision(clock)
            .addLimit(Bandwidth.builder()
                .capacity(max)
                .refillGreedy(max, Duration.ofSeconds(windowSeconds))
                .build())
            .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return new BucketResult(probe.isConsumed(), probe.getRemainingTokens(), probe.getNanosToWaitForRefill());
    }

    @Override
    public void clearAll() {
        buckets.invalidateAll();
    }
}