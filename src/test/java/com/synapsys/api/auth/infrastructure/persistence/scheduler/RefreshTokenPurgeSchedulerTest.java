package com.synapsys.api.auth.infrastructure.persistence.scheduler;

import com.synapsys.api.auth.domain.port.out.RefreshTokenConfigPort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenMaintenancePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenPurgeSchedulerTest {

    @Mock
    RefreshTokenMaintenancePort refreshTokenRepository;

    private static final int EXPIRY_DAYS = 30;

    private RefreshTokenPurgeScheduler scheduler() {
        RefreshTokenConfigPort tokenConfig = () -> EXPIRY_DAYS;
        return new RefreshTokenPurgeScheduler(refreshTokenRepository, tokenConfig);
    }

    @Test
    void purgeExpiredTokens_delegatesToRepository() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any(Instant.class), any(Instant.class))).thenReturn(5);

        scheduler().purgeExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevoked(any(Instant.class), any(Instant.class));
    }

    @Test
    void purgeExpiredTokens_cutoffMatchesRefreshTokenExpiryDays() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any(Instant.class), any(Instant.class))).thenReturn(0);
        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        scheduler().purgeExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevoked(nowCaptor.capture(), cutoffCaptor.capture());
        long daysBetween = Duration.between(cutoffCaptor.getValue(), nowCaptor.getValue()).toDays();
        assertThat(daysBetween).isBetween((long) EXPIRY_DAYS - 1, (long) EXPIRY_DAYS + 1);
    }
}