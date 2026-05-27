package com.synapsys.api.auth.infrastructure.persistence.repository;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.IntegrationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RefreshTokenRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired UserJpaRepository userJpaRepository;

    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        refreshTokenJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setUsername("repo-test-user");
        user.setEmail("repo@test.com");
        user.setPasswordHash("hash");
        user.setRole(Role.USER);
        savedUser = userJpaRepository.save(user);
    }

    @Test
    void deleteExpiredAndOldRevoked_deletesRevokedToken_whenLastUsedAtNullAndCreatedAtBeforeCutoff() {
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        RefreshTokenEntity token = new RefreshTokenEntity(savedUser.getId(), "hash-null-used", futureExpiry);
        token.setRevoked(true);
        refreshTokenJpaRepository.save(token);

        Instant now = Instant.now();
        Instant cutoffAfterCreation = now.plusSeconds(60);

        int deleted = refreshTokenJpaRepository.deleteExpiredAndOldRevoked(now, cutoffAfterCreation);

        assertThat(deleted).isEqualTo(1);
        assertThat(refreshTokenJpaRepository.findByTokenHash("hash-null-used")).isEmpty();
    }

    @Test
    void deleteExpiredAndOldRevoked_keepsRevokedToken_whenLastUsedAtNullButCreatedAtAfterCutoff() {
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        RefreshTokenEntity token = new RefreshTokenEntity(savedUser.getId(), "hash-fresh", futureExpiry);
        token.setRevoked(true);
        refreshTokenJpaRepository.save(token);

        Instant now = Instant.now();
        Instant cutoffBeforeCreation = now.minusSeconds(3600);

        int deleted = refreshTokenJpaRepository.deleteExpiredAndOldRevoked(now, cutoffBeforeCreation);

        assertThat(deleted).isEqualTo(0);
        assertThat(refreshTokenJpaRepository.findByTokenHash("hash-fresh")).isPresent();
    }
}