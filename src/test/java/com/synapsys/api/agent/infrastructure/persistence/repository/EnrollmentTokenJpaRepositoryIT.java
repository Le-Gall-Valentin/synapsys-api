package com.synapsys.api.agent.infrastructure.persistence.repository;

import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class EnrollmentTokenJpaRepositoryIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired EnrollmentTokenJpaRepository repository;
    @Autowired UserIdentityJpaRepository userRepository;

    private UUID creator;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        userRepository.deleteAll();
        UserIdentityEntity admin = new UserIdentityEntity();
        admin.setUsername("token-admin");
        admin.setEmail("token-admin@test.com");
        admin.setRole(Role.ADMIN);
        creator = userRepository.saveAndFlush(admin).getId();
    }

    private EnrollmentTokenEntity save(String hash, Instant expiresAt) {
        return repository.saveAndFlush(new EnrollmentTokenEntity("web-01", hash, expiresAt, creator));
    }

    @Test
    void findByTokenHash_returnsSavedToken() {
        save("hash-1", Instant.now().plus(24, ChronoUnit.HOURS));
        assertThat(repository.findByTokenHash("hash-1")).isPresent();
        assertThat(repository.findByTokenHash("missing")).isEmpty();
    }

    @Test
    void markConsumed_activeToken_returns1_thenZeroOnSecondCall() {
        EnrollmentTokenEntity t = save("hash-2", Instant.now().plus(24, ChronoUnit.HOURS));
        Instant now = Instant.now();
        assertThat(repository.markConsumed(t.getId(), now)).isEqualTo(1);
        assertThat(repository.markConsumed(t.getId(), now)).isZero();
        assertThat(repository.findById(t.getId()).orElseThrow().getConsumedAt()).isNotNull();
    }

    @Test
    void markConsumed_expiredToken_returnsZero() {
        EnrollmentTokenEntity t = save("hash-3", Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(repository.markConsumed(t.getId(), Instant.now())).isZero();
    }

    @Test
    void markRevoked_activeToken_returns1_andRecordsRevoker() {
        EnrollmentTokenEntity t = save("hash-4", Instant.now().plus(24, ChronoUnit.HOURS));
        assertThat(repository.markRevoked(t.getId(), creator, Instant.now())).isEqualTo(1);
        assertThat(repository.findById(t.getId()).orElseThrow().getRevokedBy()).isEqualTo(creator);
    }

    @Test
    void markRevoked_consumedToken_returnsZero() {
        EnrollmentTokenEntity t = save("hash-5", Instant.now().plus(24, ChronoUnit.HOURS));
        repository.markConsumed(t.getId(), Instant.now());
        assertThat(repository.markRevoked(t.getId(), creator, Instant.now())).isZero();
    }
}
