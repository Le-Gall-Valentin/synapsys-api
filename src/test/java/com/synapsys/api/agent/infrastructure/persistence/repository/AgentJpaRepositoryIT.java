package com.synapsys.api.agent.infrastructure.persistence.repository;

import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.infrastructure.persistence.entity.AgentEntity;
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
class AgentJpaRepositoryIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired AgentJpaRepository agents;
    @Autowired com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository tokens;
    @Autowired UserIdentityJpaRepository users;

    private UUID userId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        agents.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
        UserIdentityEntity u = new UserIdentityEntity();
        u.setUsername("agent-admin");
        u.setEmail("agent-admin@test.com");
        u.setRole(Role.ADMIN);
        userId = users.saveAndFlush(u).getId();
        tokenId = tokens.saveAndFlush(new EnrollmentTokenEntity(
            "web-01", "hash-tok", Instant.now().plus(24, ChronoUnit.HOURS), userId)).getId();
    }

    private AgentEntity newAgent(String serverName, String publicKey, String fingerprint) {
        return new AgentEntity(serverName, publicKey, fingerprint, tokenId, userId);
    }

    @Test
    void insert_defaultsToEnrolled_andIsFound() {
        AgentEntity a = agents.saveAndFlush(newAgent("web-01", "pk-1", "fp-1"));
        assertThat(agents.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(AgentLifecycleStatus.ENROLLED);
        assertThat(agents.existsByPublicKey("pk-1")).isTrue();
        assertThat(agents.existsByPublicKey("pk-x")).isFalse();
    }

    @Test
    void serverName_partialUnique_blocksSecondNonRevoked_allowsAfterRevoke() {
        agents.saveAndFlush(newAgent("web-01", "pk-1", "fp-1"));
        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> agents.saveAndFlush(newAgent("web-01", "pk-2", "fp-2")))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // revoke the first, then the same name is free again
        AgentEntity first = agents.findAll().get(0);
        agents.markRevoked(first.getId(), userId, Instant.now(), AgentLifecycleStatus.REVOKED);
        assertThat(agents.saveAndFlush(newAgent("web-01", "pk-3", "fp-3")).getId()).isNotNull();
    }

    @Test
    void markRevoked_enrolledToRevoked_idempotent() {
        AgentEntity a = agents.saveAndFlush(newAgent("web-01", "pk-1", "fp-1"));
        assertThat(agents.markRevoked(a.getId(), userId, Instant.now(), AgentLifecycleStatus.REVOKED)).isEqualTo(1);
        assertThat(agents.markRevoked(a.getId(), userId, Instant.now(), AgentLifecycleStatus.REVOKED)).isZero();
    }

    @Test
    void deleteIfRevoked_onlyWhenRevoked() {
        AgentEntity a = agents.saveAndFlush(newAgent("web-01", "pk-1", "fp-1"));
        assertThat(agents.deleteIfRevoked(a.getId())).isZero();
        agents.markRevoked(a.getId(), userId, Instant.now(), AgentLifecycleStatus.REVOKED);
        assertThat(agents.deleteIfRevoked(a.getId())).isEqualTo(1);
    }

    @Test
    void markConnected_setsFirstConnectedOnce() {
        AgentEntity a = agents.saveAndFlush(newAgent("web-01", "pk-1", "fp-1"));
        Instant t1 = Instant.now().minusSeconds(100).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Instant t2 = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        agents.markConnected(a.getId(), t1, "1.1.1.1");
        agents.markConnected(a.getId(), t2, "2.2.2.2");
        AgentEntity reloaded = agents.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getFirstConnectedAt()).isEqualTo(t1);
        assertThat(reloaded.getIpAddress()).isEqualTo("2.2.2.2");
    }

    @Test
    void findAllNonRevoked_and_countRevoked() {
        agents.saveAndFlush(newAgent("web-01", "pk-1", "fp-1"));
        AgentEntity b = agents.saveAndFlush(newAgent("web-02", "pk-2", "fp-2"));
        agents.markRevoked(b.getId(), userId, Instant.now(), AgentLifecycleStatus.REVOKED);
        assertThat(agents.findAllNonRevoked()).hasSize(1);
        assertThat(agents.countRevoked()).isEqualTo(1);
    }
}
