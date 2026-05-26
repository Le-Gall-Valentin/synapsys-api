package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.RefreshToken;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryAdapterTest {

    @Mock RefreshTokenJpaRepository jpa;
    @InjectMocks RefreshTokenRepositoryAdapter adapter;

    @Test
    void findByTokenHash_mapsEntityToDomain() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        RefreshTokenEntity entity = new RefreshTokenEntity(userId, "hash123", expiresAt);
        when(jpa.findByTokenHash("hash123")).thenReturn(Optional.of(entity));

        Optional<RefreshToken> result = adapter.findByTokenHash("hash123");

        assertThat(result).isPresent();
        assertThat(result.get().tokenHash()).isEqualTo("hash123");
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void findByTokenHash_notFound_returnsEmpty() {
        when(jpa.findByTokenHash("unknown")).thenReturn(Optional.empty());

        assertThat(adapter.findByTokenHash("unknown")).isEmpty();
    }
}