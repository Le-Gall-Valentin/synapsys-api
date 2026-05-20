package com.synapsys.api.auth.infrastructure.persistence.scheduler;

import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RefreshTokenPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurgeScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenPurgeScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(30, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked(now, cutoff);
        log.info("Purged {} expired/revoked refresh tokens", deleted);
    }
}