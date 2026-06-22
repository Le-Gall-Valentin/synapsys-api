package com.synapsys.api.agent.infrastructure.persistence.adapter;

import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.NewAgent;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.model.SortRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class AgentRepositoryAdapterIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired AgentRepositoryAdapter adapter;
    @Autowired EnrollmentTokenJpaRepository tokens;
    @Autowired UserIdentityJpaRepository users;
    @Autowired com.synapsys.api.agent.infrastructure.persistence.repository.AgentJpaRepository agentJpa;

    private UUID userId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        agentJpa.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
        UserIdentityEntity u = new UserIdentityEntity();
        u.setUsername("adp-admin");
        u.setEmail("adp-admin@test.com");
        u.setRole(Role.ADMIN);
        userId = users.saveAndFlush(u).getId();
        tokenId = tokens.saveAndFlush(new EnrollmentTokenEntity(
            "web-01", "h", Instant.now().plus(24, ChronoUnit.HOURS), userId)).getId();
    }

    private NewAgent newAgent(String name, byte first) {
        byte[] pk = new byte[32];
        pk[0] = first;
        return new NewAgent(name, pk, "fp-" + first, tokenId, userId);
    }

    @Test
    void insert_roundTripsPublicKeyBytes() {
        Agent a = adapter.insert(newAgent("web-01", (byte) 7));
        Agent loaded = adapter.findById(a.id()).orElseThrow();
        assertThat(loaded.publicKey()[0]).isEqualTo((byte) 7);
        assertThat(adapter.existsByPublicKey(loaded.publicKey())).isTrue();
    }

    @Test
    void insert_duplicatePublicKey_throwsPublicKeyAlreadyRegistered() {
        adapter.insert(newAgent("web-01", (byte) 1));
        assertThatThrownBy(() -> adapter.insert(newAgent("web-02", (byte) 1)))
            .isInstanceOf(AgentException.PublicKeyAlreadyRegistered.class);
    }

    @Test
    void insert_duplicateNonRevokedServerName_throwsServerNameInUse() {
        adapter.insert(newAgent("web-01", (byte) 1));
        assertThatThrownBy(() -> adapter.insert(newAgent("web-01", (byte) 2)))
            .isInstanceOf(AgentException.ServerNameInUse.class);
    }

    @Test
    void markRevoked_then_deletable_then_serverNameFree() {
        Agent a = adapter.insert(newAgent("web-01", (byte) 1));
        assertThat(adapter.markRevoked(a.id(), userId, Instant.now())).isTrue();
        assertThat(adapter.delete(a.id())).isTrue();
        // name free again after the revoked agent is gone
        assertThat(adapter.insert(newAgent("web-01", (byte) 9)).id()).isNotNull();
    }

    @Test
    void delete_nonRevoked_returnsFalse() {
        Agent a = adapter.insert(newAgent("web-01", (byte) 1));
        assertThat(adapter.delete(a.id())).isFalse();
    }

    @Test
    void countRevoked_and_findAll() {
        adapter.insert(newAgent("web-01", (byte) 1));
        Agent b = adapter.insert(newAgent("web-02", (byte) 2));
        adapter.markRevoked(b.id(), userId, Instant.now());
        assertThat(adapter.countRevoked()).isEqualTo(1);
        PageResult<Agent> all = adapter.findAll(0, 20, SortRequest.descBy("enrolledAt"));
        assertThat(all.totalElements()).isEqualTo(2);
        assertThat(adapter.findAllNonRevoked()).hasSize(1);
    }
}
