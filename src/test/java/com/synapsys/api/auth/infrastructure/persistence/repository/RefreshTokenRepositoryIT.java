package com.synapsys.api.auth.infrastructure.persistence.repository;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "synapsys.jwt.secret=integration-test-secret-at-least-32-chars!",
    "synapsys.jwt.expiry-minutes=15",
    "synapsys.refresh-token.expiry-days=30",
    "synapsys.cookie.secure=false",
    "synapsys.seed.username=it-admin",
    "synapsys.seed.email=it-admin@test.local",
    "synapsys.seed.password=integration-test-seed-password",
    "synapsys.cors.allowed-origins=",
    "spring.jpa.hibernate.ddl-auto=none"
})
@Testcontainers
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