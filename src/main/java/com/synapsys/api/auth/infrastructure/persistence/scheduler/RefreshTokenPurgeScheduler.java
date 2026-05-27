package com.synapsys.api.auth.infrastructure.persistence.scheduler;

import com.synapsys.api.auth.domain.port.out.RefreshTokenConfigPort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenMaintenancePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RefreshTokenPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurgeScheduler.class);

    private final RefreshTokenMaintenancePort refreshTokenMaintenance;
    private final int refreshTokenExpiryDays;

    public RefreshTokenPurgeScheduler(RefreshTokenMaintenancePort refreshTokenMaintenance,
                                      RefreshTokenConfigPort tokenConfig) {
        this.refreshTokenMaintenance = refreshTokenMaintenance;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
    }

    @Scheduled(cron = "${synapsys.refresh-token.purge-cron:0 0 3 * * *}")
    public void purgeExpiredTokens() {
        try {
            Instant now = Instant.now();
            Instant cutoff = now.minus(refreshTokenExpiryDays, ChronoUnit.DAYS);
            int deleted = refreshTokenMaintenance.deleteExpiredAndRevoked(now, cutoff);
            log.info("Purged {} expired/revoked refresh tokens", deleted);
        } catch (Exception e) {
            log.error("Refresh token purge failed — will retry at next scheduled run", e);
        }
    }
}