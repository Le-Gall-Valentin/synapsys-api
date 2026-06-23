package com.synapsys.api.identity.infrastructure.persistence.adapter;

import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class UserIdentityRepositoryAdapterIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired UserIdentityRepositoryAdapter repositoryAdapter;
    @Autowired UserIdentityJpaRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void findByIds_returnsOnlyNonDeletedMatchingUsers() {
        UUID a = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("alice", "alice@example.com", Role.USER)).id();
        UUID b = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("bob", "bob@example.com", Role.USER)).id();
        UUID absent = UUID.randomUUID();

        List<User> found = repositoryAdapter.findByIds(List.of(a, b, absent));

        assertThat(found).extracting(User::username)
            .containsExactlyInAnyOrder("alice", "bob");
    }
}