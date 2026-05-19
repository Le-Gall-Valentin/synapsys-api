package com.synapsys.api.auth.infrastructure.persistence.scheduler;

import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
    RefreshTokenJpaRepository jpa;

    @InjectMocks
    RefreshTokenPurgeScheduler scheduler;

    @Test
    void purgeExpiredTokens_delegatesToRepository() {
        when(jpa.deleteExpiredAndOldRevoked(any(Instant.class), any(Instant.class))).thenReturn(5);

        scheduler.purgeExpiredTokens();

        verify(jpa).deleteExpiredAndOldRevoked(any(Instant.class), any(Instant.class));
    }

    @Test
    void purgeExpiredTokens_cutoffIsApproximately30DaysInPast() {
        when(jpa.deleteExpiredAndOldRevoked(any(Instant.class), any(Instant.class))).thenReturn(0);
        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        scheduler.purgeExpiredTokens();

        verify(jpa).deleteExpiredAndOldRevoked(nowCaptor.capture(), cutoffCaptor.capture());
        long daysBetween = Duration.between(cutoffCaptor.getValue(), nowCaptor.getValue()).toDays();
        assertThat(daysBetween).isBetween(29L, 31L);
    }
}