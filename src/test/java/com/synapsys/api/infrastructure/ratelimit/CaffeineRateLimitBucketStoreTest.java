package com.synapsys.api.infrastructure.ratelimit;

import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineRateLimitBucketStoreTest {

    private final AtomicLong virtualNanos = new AtomicLong(0);
    private CaffeineRateLimitBucketStore store;

    @BeforeEach
    void setUp() {
        store = new CaffeineRateLimitBucketStore(new io.github.bucket4j.TimeMeter() {
            @Override
            public long currentTimeNanos() { return virtualNanos.get(); }
            @Override
            public boolean isWallClockBased() { return false; }
        });
    }

    @Test
    void firstMaxRequestsAreAllowed() {
        for (int i = 0; i < 10; i++) {
            assertThat(store.tryConsume("AuthController.login:IP:1.2.3.4", 10, 60).allowed())
                .as("attempt %d", i + 1).isTrue();
        }
    }

    @Test
    void requestBeyondMaxIsBlocked() {
        for (int i = 0; i < 10; i++) {
            store.tryConsume("AuthController.login:IP:1.2.3.4", 10, 60);
        }
        var result = store.tryConsume("AuthController.login:IP:1.2.3.4", 10, 60);
        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isZero();
        assertThat(result.nanosToWaitForRefill()).isPositive();
    }

    @Test
    void remainingDecrementsWithEachRequest() {
        var first = store.tryConsume("AuthController.login:IP:2.2.2.2", 5, 60);
        assertThat(first.remaining()).isEqualTo(4L);

        var second = store.tryConsume("AuthController.login:IP:2.2.2.2", 5, 60);
        assertThat(second.remaining()).isEqualTo(3L);
    }

    @Test
    void tokensRefillAfterWindow() {
        for (int i = 0; i < 5; i++) {
            store.tryConsume("AuthController.login:IP:3.3.3.3", 5, 60);
        }
        assertThat(store.tryConsume("AuthController.login:IP:3.3.3.3", 5, 60).allowed()).isFalse();

        // Advance virtual clock past the window
        virtualNanos.set(61L * 1_000_000_000L);

        assertThat(store.tryConsume("AuthController.login:IP:3.3.3.3", 5, 60).allowed()).isTrue();
    }

    @Test
    void differentKeysDontInterfere() {
        for (int i = 0; i < 10; i++) {
            store.tryConsume("AuthController.login:IP:4.4.4.4", 10, 60);
        }
        assertThat(store.tryConsume("AuthController.login:IP:5.5.5.5", 10, 60).allowed()).isTrue();
    }

    @Test
    void clearAllResetsAllBuckets() {
        for (int i = 0; i < 10; i++) {
            store.tryConsume("AuthController.login:IP:6.6.6.6", 10, 60);
        }
        store.clearAll();
        assertThat(store.tryConsume("AuthController.login:IP:6.6.6.6", 10, 60).allowed()).isTrue();
    }

    @Test
    void concurrentRequestsRespectLimit() throws InterruptedException {
        CaffeineRateLimitBucketStore realStore = new CaffeineRateLimitBucketStore();
        int threads = 30;
        int max = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                if (realStore.tryConsume("AuthController.login:IP:concurrent", max, 60).allowed()) {
                    allowed.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(allowed.get()).isLessThanOrEqualTo(max);
    }
}