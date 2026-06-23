package com.synapsys.api.agent.infrastructure.persistence.adapter;

import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.NewEnrollmentToken;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentTokenRepositoryAdapterTest {

    @Mock EnrollmentTokenJpaRepository jpa;
    private EnrollmentTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() { adapter = new EnrollmentTokenRepositoryAdapter(jpa); }

    @Test
    void markConsumed_returnsTrueWhenOneRowChanged() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        when(jpa.markConsumed(id, now)).thenReturn(1);
        assertThat(adapter.markConsumed(id, now)).isTrue();
        when(jpa.markConsumed(id, now)).thenReturn(0);
        assertThat(adapter.markConsumed(id, now)).isFalse();
    }

    @Test
    void markRevoked_returnsTrueWhenOneRowChanged() {
        UUID id = UUID.randomUUID();
        UUID by = UUID.randomUUID();
        Instant now = Instant.now();
        when(jpa.markRevoked(id, by, now)).thenReturn(1);
        assertThat(adapter.markRevoked(id, by, now)).isTrue();
        when(jpa.markRevoked(id, by, now)).thenReturn(0);
        assertThat(adapter.markRevoked(id, by, now)).isFalse();
    }

    @Test
    void save_mapsEntityBackToDomain() {
        var saved = new EnrollmentTokenEntity("web-01", "hash", Instant.now().plusSeconds(3600), UUID.randomUUID());
        when(jpa.saveAndFlush(any())).thenReturn(saved);
        EnrollmentToken result = adapter.save(new NewEnrollmentToken("web-01", "hash", saved.getExpiresAt(), saved.getCreatedBy()));
        assertThat(result.serverName()).isEqualTo("web-01");
        assertThat(result.createdBy()).isEqualTo(saved.getCreatedBy());
    }

    @Test
    void findByTokenHash_mapsToDomain() {
        var entity = new EnrollmentTokenEntity("web-01", "hash", Instant.now().plusSeconds(3600), UUID.randomUUID());
        when(jpa.findByTokenHash("hash")).thenReturn(Optional.of(entity));
        Optional<EnrollmentToken> result = adapter.findByTokenHash("hash");
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().serverName()).isEqualTo("web-01");
    }
}
